package com.doc.impl.project;


import com.doc.dto.GovernmentFeePostingRequestDto;
import com.doc.dto.GovernmentFeePostingResponseDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;
import com.doc.em.*;
import com.doc.entity.project.Project;
import com.doc.entity.project.activity.ProjectExpense;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.feign.AccountExpenseFeignClient;
import com.doc.repository.UserRepository;
import com.doc.repository.projectRepo.activity.ProjectExpenseRepository;
import com.doc.service.ExpenseAccountPostingService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseAccountPostingServiceImpl
        implements ExpenseAccountPostingService {

    private final ProjectExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final AccountExpenseFeignClient accountExpenseFeignClient;


    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void postGovernmentFeeExpense(Long expenseId) {

        ProjectExpense expense = expenseRepository
                .findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expense not found with ID: " + expenseId,
                        "ERR_EXPENSE_NOT_FOUND"
                ));

        if (expense.getExpenseCategory() !=
                ExpenseCategory.GOVERNMENT_FEE) {

            expense.setAccountPostingStatus(
                    AccountPostingStatus.NOT_REQUIRED
            );

            expenseRepository.save(expense);
            return;
        }

        if (expense.getExpensePaidBy() ==
                ExpensePaidBy.CLIENT) {

            expense.setAccountPostingStatus(
                    AccountPostingStatus.SKIPPED
            );

            expense.setPaymentStatus(
                    ExpensePaymentStatus.CLIENT_PAID
            );

            expense.setAccountPostingError(null);

            expenseRepository.save(expense);
            return;
        }

        if (expense.getApprovalStatus() !=
                ApprovalStatus.APPROVED) {

            throw new ValidationException(
                    "Only approved expenses can be posted to Account Service",
                    "ERR_EXPENSE_NOT_APPROVED"
            );
        }

        /*
         * Already successfully posted.
         */
        if (expense.getAccountPostingStatus() ==
                AccountPostingStatus.POSTED
                && expense.getAccountVoucherId() != null) {

            return;
        }

        expense.setAccountPostingStatus(
                AccountPostingStatus.PENDING
        );

        expense.setAccountPostingError(null);

        expenseRepository.save(expense);

        Project project = expense.getProject();

        GovernmentFeePostingRequestDto request =
                GovernmentFeePostingRequestDto.builder()
                        .operationExpenseId(expense.getId())
                        .projectId(
                                project != null
                                        ? project.getId()
                                        : null
                        )
                        .projectNo(
                                project != null
                                        ? project.getProjectNo()
                                        : null
                        )
                        .projectName(
                                project != null
                                        ? project.getName()
                                        : null
                        )
                        .expenseCategory(
                                expense.getExpenseCategory().name()
                        )
                        .approvedAmount(
                                expense.getApprovedAmount()
                        )
                        .currencyCode(
                                expense.getCurrencyCode()
                        )
                        .expenseDate(
                                expense.getExpenseDate() != null
                                        ? expense.getExpenseDate()
                                        .toLocalDate()
                                        : LocalDate.now()
                        )
                        .paidBy(expense.getExpensePaidBy())
                        .approvedByUserId(
                                expense.getAccountsActionByUserId()
                        )
                        .approvedByUserName(
                                expense.getAccountsActionByUserName()
                        )
                        .narration(
                                buildNarration(expense)
                        )
                        .build();

        try {

            GovernmentFeePostingResponseDto response =
                    accountExpenseFeignClient
                            .postGovernmentFeeExpense(request);

            if (response == null) {
                markFailed(
                        expense,
                        "Empty response received from Account Service"
                );
                return;
            }

            if ("SKIPPED_CLIENT_PAID".equalsIgnoreCase(
                    response.getPostingStatus()
            )) {

                expense.setAccountPostingStatus(
                        AccountPostingStatus.SKIPPED
                );

                expense.setPaymentStatus(
                        ExpensePaymentStatus.CLIENT_PAID
                );

                expense.setPaidAmount(
                        expense.getApprovedAmount()
                );

                expense.setAccountPostingError(null);

            } else {

                expense.setAccountPostingStatus(
                        AccountPostingStatus.POSTED
                );

                expense.setAccountVoucherId(
                        response.getVoucherId()
                );

                expense.setAccountVoucherNumber(
                        response.getVoucherNumber()
                );

                expense.setAccountPostedAt(
                        response.getPostedAt() != null
                                ? response.getPostedAt()
                                : LocalDateTime.now()
                );

                expense.setAccountPostingError(null);
            }

            expenseRepository.save(expense);

        } catch (FeignException exception) {

            String error =
                    "Account Service posting failed. HTTP status: "
                            + exception.status()
                            + ", message: "
                            + exception.getMessage();

            log.error(
                    "Government fee posting failed | expenseId={} | error={}",
                    expenseId,
                    error,
                    exception
            );

            markFailed(expense, error);

        } catch (Exception exception) {

            log.error(
                    "Unexpected government fee posting failure | expenseId={}",
                    expenseId,
                    exception
            );

            markFailed(
                    expense,
                    exception.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public ProjectExpenseResponseDto retryGovernmentFeePosting(
            Long expenseId,
            Long userId
    ) {

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_USER_NOT_FOUND"
                ));

        ProjectExpense expense = expenseRepository
                .findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expense not found with ID: " + expenseId,
                        "ERR_EXPENSE_NOT_FOUND"
                ));

        if (expense.getAccountPostingStatus() ==
                AccountPostingStatus.POSTED) {

            throw new ValidationException(
                    "Expense is already posted to Account Service",
                    "ERR_EXPENSE_ALREADY_POSTED"
            );
        }

        if (expense.getExpensePaidBy() !=
                ExpensePaidBy.COMPANY) {

            throw new ValidationException(
                    "Account posting is not required because client paid the expense",
                    "ERR_ACCOUNT_POSTING_NOT_REQUIRED"
            );
        }

        postGovernmentFeeExpense(expenseId);

        ProjectExpense refreshed = expenseRepository
                .findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expense not found after retry",
                        "ERR_EXPENSE_NOT_FOUND"
                ));

        return mapMinimalResponse(refreshed);
    }

    private void markFailed(
            ProjectExpense expense,
            String error
    ) {

        expense.setAccountPostingStatus(
                AccountPostingStatus.FAILED
        );

        expense.setAccountPostingError(
                truncate(error, 2000)
        );

        expenseRepository.save(expense);
    }

    private String buildNarration(
            ProjectExpense expense
    ) {

        Project project = expense.getProject();

        return "Government fee approved for project "
                + (project != null
                ? project.getProjectNo()
                : "N/A")
                + ", expense ID "
                + expense.getId()
                + ". "
                + expense.getRemark();
    }

    private String truncate(
            String value,
            int maximumLength
    ) {

        if (value == null) {
            return null;
        }

        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength);
    }

    private ProjectExpenseResponseDto mapMinimalResponse(
            ProjectExpense expense
    ) {

        ProjectExpenseResponseDto dto =
                new ProjectExpenseResponseDto();

        dto.setExpenseId(expense.getId());

        dto.setApprovalStatus(
                expense.getApprovalStatus()
        );

        dto.setPaymentStatus(
                expense.getPaymentStatus()
        );

        dto.setExpensePaidBy(
                expense.getExpensePaidBy()
        );

        dto.setAccountPostingStatus(
                expense.getAccountPostingStatus()
        );

        dto.setAccountVoucherId(
                expense.getAccountVoucherId()
        );

        dto.setAccountVoucherNumber(
                expense.getAccountVoucherNumber()
        );

        dto.setAccountPostedAt(
                expense.getAccountPostedAt()
        );

        dto.setAccountPostingError(
                expense.getAccountPostingError()
        );

        return dto;
    }
}