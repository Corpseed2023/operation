package com.doc.impl.project;

import com.doc.dto.GovernmentFeeFundTransferPostingRequestDto;
import com.doc.dto.GovernmentFeeFundTransferPostingResponseDto;
import com.doc.dto.GovernmentFeePostingRequestDto;
import com.doc.dto.GovernmentFeePostingResponseDto;
import com.doc.dto.project.activity.expense.GovernmentFeeFundTransferRequestDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;
import com.doc.em.AccountPostingStatus;
import com.doc.em.ApprovalStatus;
import com.doc.em.ExpenseCategory;
import com.doc.em.ExpensePaidBy;
import com.doc.em.ExpensePaymentStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Operation Service -> Account Service integration for government fees.
 *
 * Step 3 posts the approval vouchers.
 * Step 4 posts only an inter-bank CONTRA voucher and changes payment status
 * from PENDING to PROCESSING. It never changes Government Fee Payable and it
 * never marks the expense PAID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseAccountPostingServiceImpl
        implements ExpenseAccountPostingService {

    private final ProjectExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final AccountExpenseFeignClient accountExpenseFeignClient;

    // =========================================================
    // STEP 3 - APPROVAL POSTING
    // =========================================================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void postGovernmentFeeExpense(Long expenseId) {

        ProjectExpense expense = getExpense(expenseId);

        if (expense.getExpenseCategory() != ExpenseCategory.GOVERNMENT_FEE) {
            expense.setAccountPostingStatus(AccountPostingStatus.NOT_REQUIRED);
            expense.setAccountPostingError(null);
            expenseRepository.save(expense);
            return;
        }

        if (isClientDirect(expense.getExpensePaidBy())) {
            expense.setAccountPostingStatus(AccountPostingStatus.SKIPPED);
            expense.setPaymentStatus(ExpensePaymentStatus.CLIENT_PAID);
            expense.setPaidAmount(expense.getApprovedAmount());
            expense.setPaymentCompletedDate(LocalDateTime.now());
            expense.setAccountPostingError(null);
            expenseRepository.save(expense);
            return;
        }

        validateApprovedGovernmentFee(expense);

        if (expense.getAccountPostingStatus() == AccountPostingStatus.POSTED
                && expense.getInitialJournalVoucherId() != null) {
            return;
        }

        expense.setAccountPostingStatus(AccountPostingStatus.PENDING);
        expense.setAccountPostingError(null);
        expenseRepository.save(expense);

        GovernmentFeePostingRequestDto request =
                buildGovernmentFeePostingRequest(expense);

        try {
            GovernmentFeePostingResponseDto response =
                    accountExpenseFeignClient.postGovernmentFeeExpense(request);

            if (response == null) {
                markGovernmentFeePostingFailed(
                        expense,
                        "Empty response received from Account Service"
                );
                return;
            }

            if ("SKIPPED_CLIENT_DIRECT".equalsIgnoreCase(response.getPostingStatus())) {
                expense.setAccountPostingStatus(AccountPostingStatus.SKIPPED);
                expense.setAccountPostingError(null);
                expenseRepository.save(expense);
                return;
            }

            if (!"POSTED".equalsIgnoreCase(response.getPostingStatus())
                    && !"ALREADY_POSTED".equalsIgnoreCase(response.getPostingStatus())) {

                markGovernmentFeePostingFailed(
                        expense,
                        "Unsupported Account Service posting status: "
                                + response.getPostingStatus()
                );
                return;
            }

            expense.setAccountPostingStatus(AccountPostingStatus.POSTED);

            // Entry A; null for COMPANY-funded expenses.
            expense.setReceiptVoucherId(response.getReceiptVoucherId());
            expense.setReceiptVoucherNumber(response.getReceiptVoucherNumber());

            // Entry B; always the primary approval voucher.
            expense.setInitialJournalVoucherId(response.getJournalVoucherId());
            expense.setInitialJournalVoucherNumber(response.getJournalVoucherNumber());

            // Retained for old UI/API compatibility.
            expense.setAccountVoucherId(response.getJournalVoucherId());
            expense.setAccountVoucherNumber(response.getJournalVoucherNumber());
            expense.setAccountPostedAt(
                    response.getPostedAt() != null
                            ? response.getPostedAt()
                            : LocalDateTime.now()
            );
            expense.setAccountPostingError(null);

            expenseRepository.save(expense);

        } catch (FeignException exception) {
            markGovernmentFeePostingFailed(
                    expense,
                    "Account Service posting failed. HTTP status: "
                            + exception.status()
                            + ", message: "
                            + exception.getMessage()
            );
        } catch (Exception exception) {
            markGovernmentFeePostingFailed(expense, exception.getMessage());
        }
    }

    @Override
    @Transactional
    public ProjectExpenseResponseDto retryGovernmentFeePosting(
            Long expenseId,
            Long userId
    ) {

        validateActiveUser(userId);

        ProjectExpense expense = getExpense(expenseId);

        if (expense.getAccountPostingStatus() == AccountPostingStatus.POSTED) {
            throw new ValidationException(
                    "Government-fee expense is already posted to Account Service",
                    "ERR_EXPENSE_ALREADY_POSTED"
            );
        }

        validateApprovedGovernmentFee(expense);

        postGovernmentFeeExpense(expenseId);

        return mapMinimalResponse(getExpense(expenseId));
    }

    // =========================================================
    // STEP 4 - FUND TRANSFER / CONTRA VOUCHER
    // =========================================================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProjectExpenseResponseDto transferGovernmentFeeFunds(
            Long expenseId,
            Long userId,
            GovernmentFeeFundTransferRequestDto request
    ) {

        validateActiveUser(userId);

        if (request == null) {
            throw new ValidationException(
                    "Fund transfer request is required",
                    "ERR_FUND_TRANSFER_REQUEST_REQUIRED"
            );
        }

        ProjectExpense expense = getExpense(expenseId);

        validateFundTransferEligibility(expense, request);

        /*
         * Idempotent return. A successful Contra must never be posted again.
         */
        if (expense.getFundTransferPostingStatus() == AccountPostingStatus.POSTED
                && expense.getFundTransferVoucherId() != null) {
            return mapMinimalResponse(expense);
        }

        expense.setFundTransferPostingStatus(AccountPostingStatus.PENDING);
        expense.setFundTransferPostingError(null);
        expense.setFundTransferFromBankLedgerId(request.getFromBankLedgerId());
        expense.setFundTransferFromBankName(clean(request.getFromBankName()));
        expense.setFundTransferToBankLedgerId(request.getToBankLedgerId());
        expense.setFundTransferToBankName(clean(request.getToBankName()));
        expense.setFundTransferAmount(request.getAmount());
        expense.setFundTransferDate(request.getTransferDate());
        expense.setFundTransferReference(clean(request.getTransferReference()));
        expense.setFundTransferProofUrl(clean(request.getTransferProofUrl()));
        expenseRepository.save(expense);

        GovernmentFeeFundTransferPostingRequestDto postingRequest =
                buildFundTransferPostingRequest(expense, userId, request);

        try {
            GovernmentFeeFundTransferPostingResponseDto response =
                    accountExpenseFeignClient
                            .postGovernmentFeeFundTransfer(postingRequest);

            if (response == null) {
                markFundTransferFailed(
                        expense,
                        "Empty response received from Account Service"
                );
                return mapMinimalResponse(expense);
            }

            if (!"POSTED".equalsIgnoreCase(response.getPostingStatus())
                    && !"ALREADY_POSTED".equalsIgnoreCase(response.getPostingStatus())) {

                markFundTransferFailed(
                        expense,
                        "Unsupported Account Service transfer status: "
                                + response.getPostingStatus()
                );
                return mapMinimalResponse(expense);
            }

            /*
             * Entry C:
             * Dr Axis Bank
             *     Cr HDFC Bank
             *
             * No entry touches Government Fee Payable here.
             */
            expense.setFundTransferPostingStatus(AccountPostingStatus.POSTED);
            expense.setFundTransferVoucherId(response.getContraVoucherId());
            expense.setFundTransferVoucherNumber(response.getContraVoucherNumber());
            expense.setFundTransferPostingError(null);

            // Axis is now the selected government-payment bank for Step 5.
            expense.setPaymentBankLedgerId(request.getToBankLedgerId());
            expense.setPaymentBankName(clean(request.getToBankName()));

            expense.setPaymentStatus(ExpensePaymentStatus.PROCESSING);
            expense.setPaidAmount(BigDecimal.ZERO);
            expense.setPaymentCompletedDate(null);

            expenseRepository.save(expense);

            log.info(
                    "[GOVERNMENT-FEE-FUND-TRANSFER-POSTED] expenseId={} | contraVoucherId={} | contraVoucherNumber={} | fromBankLedgerId={} | toBankLedgerId={} | amount={}",
                    expense.getId(),
                    response.getContraVoucherId(),
                    response.getContraVoucherNumber(),
                    request.getFromBankLedgerId(),
                    request.getToBankLedgerId(),
                    request.getAmount()
            );

        } catch (FeignException exception) {
            markFundTransferFailed(
                    expense,
                    "Account Service fund transfer failed. HTTP status: "
                            + exception.status()
                            + ", message: "
                            + exception.getMessage()
            );
        } catch (Exception exception) {
            markFundTransferFailed(expense, exception.getMessage());
        }

        return mapMinimalResponse(expense);
    }

    private void validateFundTransferEligibility(
            ProjectExpense expense,
            GovernmentFeeFundTransferRequestDto request
    ) {

        if (expense.getExpenseCategory() != ExpenseCategory.GOVERNMENT_FEE) {
            throw new ValidationException(
                    "Fund transfer is supported only for government-fee expenses",
                    "ERR_FUND_TRANSFER_CATEGORY_NOT_SUPPORTED"
            );
        }

        if (expense.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ValidationException(
                    "Accounts approval is required before fund transfer",
                    "ERR_EXPENSE_NOT_APPROVED"
            );
        }

        if (expense.getAccountPostingStatus() != AccountPostingStatus.POSTED) {
            throw new ValidationException(
                    "Step 3 accounting posting must be completed before fund transfer",
                    "ERR_APPROVAL_POSTING_NOT_COMPLETED"
            );
        }

        if (isClientDirect(expense.getExpensePaidBy())) {
            throw new ValidationException(
                    "Fund transfer is not required because client paid the government directly",
                    "ERR_FUND_TRANSFER_NOT_REQUIRED"
            );
        }

        if (expense.getExpensePaidBy() != ExpensePaidBy.CLIENT_TO_COMPANY
                && expense.getExpensePaidBy() != ExpensePaidBy.COMPANY) {
            throw new ValidationException(
                    "Fund transfer is not supported for expense paid by value: "
                            + expense.getExpensePaidBy(),
                    "ERR_INVALID_EXPENSE_PAID_BY"
            );
        }

        if (expense.getPaymentStatus() != ExpensePaymentStatus.PENDING) {
            throw new ValidationException(
                    "Fund transfer can be started only when payment status is PENDING",
                    "ERR_INVALID_PAYMENT_STATUS"
            );
        }

        if (request.getFromBankLedgerId() == null
                || request.getToBankLedgerId() == null
                || request.getFromBankLedgerId() <= 0
                || request.getToBankLedgerId() <= 0) {
            throw new ValidationException(
                    "Both source and destination bank ledger IDs are required",
                    "ERR_BANK_LEDGER_REQUIRED"
            );
        }

        if (request.getFromBankLedgerId().equals(request.getToBankLedgerId())) {
            throw new ValidationException(
                    "Source bank and destination bank cannot be the same",
                    "ERR_SAME_BANK_TRANSFER"
            );
        }

        if (request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Fund transfer amount must be greater than zero",
                    "ERR_INVALID_FUND_TRANSFER_AMOUNT"
            );
        }

        if (expense.getApprovedAmount() == null
                || request.getAmount().compareTo(expense.getApprovedAmount()) != 0) {
            throw new ValidationException(
                    "Fund transfer amount must equal approved amount for this single-transfer flow",
                    "ERR_FUND_TRANSFER_AMOUNT_MISMATCH"
            );
        }

        if (request.getTransferDate() == null
                || request.getTransferDate().isAfter(LocalDate.now())) {
            throw new ValidationException(
                    "Fund transfer date is required and cannot be in the future",
                    "ERR_INVALID_FUND_TRANSFER_DATE"
            );
        }

        requireText(
                request.getTransferReference(),
                "Fund transfer reference is required",
                "ERR_FUND_TRANSFER_REFERENCE_REQUIRED"
        );

        /*
         * For client-funded expenses, the source must be the bank where CRT
         * declared that client money arrived. Example: HDFC -> Axis.
         */
        if (expense.getExpensePaidBy() == ExpensePaidBy.CLIENT_TO_COMPANY
                && !request.getFromBankLedgerId().equals(
                expense.getClientPaymentBankLedgerId())) {

            throw new ValidationException(
                    "Source bank must match the client funding bank selected by CRT",
                    "ERR_SOURCE_BANK_DOES_NOT_MATCH_CLIENT_FUNDING_BANK"
            );
        }
    }

    private GovernmentFeePostingRequestDto buildGovernmentFeePostingRequest(
            ProjectExpense expense
    ) {
        Project project = expense.getProject();

        return GovernmentFeePostingRequestDto.builder()
                .operationExpenseId(expense.getId())
                .projectId(project != null ? project.getId() : null)
                .projectNo(project != null ? project.getProjectNo() : null)
                .projectName(project != null ? project.getName() : null)
                .clientCompanyId(
                        project != null && project.getCompany() != null
                                ? project.getCompany().getId()
                                : null
                )
                .clientCompanyName(
                        project != null && project.getCompany() != null
                                ? project.getCompany().getName()
                                : null
                )
                .clientUnitId(
                        project != null && project.getUnit() != null
                                ? project.getUnit().getId()
                                : null
                )
                .clientUnitName(
                        project != null && project.getUnit() != null
                                ? project.getUnit().getUnitName()
                                : null
                )
                .expenseCategory(expense.getExpenseCategory().name())
                .approvedAmount(expense.getApprovedAmount())
                .currencyCode(expense.getCurrencyCode())
                .expenseDate(
                        expense.getAccountsActionDate() != null
                                ? expense.getAccountsActionDate().toLocalDate()
                                : LocalDate.now()
                )
                .paidBy(expense.getExpensePaidBy())
                .clientPaymentMode(expense.getClientPaymentMode())
                .clientPaymentBankLedgerId(expense.getClientPaymentBankLedgerId())
                .clientPaymentBankName(expense.getClientPaymentBankName())
                .clientPaymentDate(expense.getClientPaymentDate())
                .clientPaymentReference(expense.getClientPaymentReference())
                .clientPaymentProofUrl(expense.getClientPaymentProofUrl())
                .approvedByUserId(expense.getAccountsActionByUserId())
                .approvedByUserName(expense.getAccountsActionByUserName())
                .narration(buildGovernmentFeeNarration(expense))
                .build();
    }

    private GovernmentFeeFundTransferPostingRequestDto buildFundTransferPostingRequest(
            ProjectExpense expense,
            Long userId,
            GovernmentFeeFundTransferRequestDto request
    ) {
        Project project = expense.getProject();

        return GovernmentFeeFundTransferPostingRequestDto.builder()
                .operationExpenseId(expense.getId())
                .projectId(project != null ? project.getId() : null)
                .projectNo(project != null ? project.getProjectNo() : null)
                .fromBankLedgerId(request.getFromBankLedgerId())
                .fromBankName(clean(request.getFromBankName()))
                .toBankLedgerId(request.getToBankLedgerId())
                .toBankName(clean(request.getToBankName()))
                .amount(request.getAmount())
                .transferDate(request.getTransferDate())
                .transferReference(clean(request.getTransferReference()))
                .transferProofUrl(clean(request.getTransferProofUrl()))
                .transferredByUserId(userId)
                .transferredByUserName(resolveUserName(userId))
                .narration(
                        "Government-fee fund transfer for project "
                                + (project != null ? project.getProjectNo() : "N/A")
                                + ", expense ID "
                                + expense.getId()
                )
                .build();
    }

    private void validateApprovedGovernmentFee(ProjectExpense expense) {
        if (expense.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ValidationException(
                    "Only approved expenses can be posted to Account Service",
                    "ERR_EXPENSE_NOT_APPROVED"
            );
        }

        if (expense.getExpensePaidBy() != ExpensePaidBy.COMPANY
                && expense.getExpensePaidBy() != ExpensePaidBy.CLIENT_TO_COMPANY) {
            throw new ValidationException(
                    "Government-fee approval posting requires COMPANY or CLIENT_TO_COMPANY funding",
                    "ERR_INVALID_EXPENSE_PAID_BY"
            );
        }
    }

    private boolean isClientDirect(ExpensePaidBy paidBy) {
        return paidBy == ExpensePaidBy.CLIENT_DIRECT
                || paidBy == ExpensePaidBy.CLIENT;
    }

    private void validateActiveUser(Long userId) {
        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_USER_NOT_FOUND"
                ));
    }

    private String resolveUserName(Long userId) {
        return userRepository.findActiveUserById(userId)
                .map(user -> user.getFullName())
                .orElse(null);
    }

    private ProjectExpense getExpense(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expense not found with ID: " + expenseId,
                        "ERR_EXPENSE_NOT_FOUND"
                ));
    }

    private void markGovernmentFeePostingFailed(
            ProjectExpense expense,
            String error
    ) {
        expense.setAccountPostingStatus(AccountPostingStatus.FAILED);
        expense.setAccountPostingError(truncate(error, 2000));
        expenseRepository.save(expense);
    }

    private void markFundTransferFailed(
            ProjectExpense expense,
            String error
    ) {
        expense.setFundTransferPostingStatus(AccountPostingStatus.FAILED);
        expense.setFundTransferPostingError(truncate(error, 2000));
        expenseRepository.save(expense);
    }

    private String buildGovernmentFeeNarration(ProjectExpense expense) {
        Project project = expense.getProject();
        return "Government fee approved for project "
                + (project != null ? project.getProjectNo() : "N/A")
                + ", expense ID "
                + expense.getId()
                + ". "
                + clean(expense.getRemark());
    }

    private String requireText(
            String value,
            String message,
            String errorCode
    ) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new ValidationException(message, errorCode);
        }
        return cleaned;
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty()
                ? null
                : value.trim();
    }

    private String truncate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength);
    }

    private ProjectExpenseResponseDto mapMinimalResponse(ProjectExpense expense) {
        ProjectExpenseResponseDto dto = new ProjectExpenseResponseDto();

        dto.setExpenseId(expense.getId());
        dto.setApprovalStatus(expense.getApprovalStatus());
        dto.setPaymentStatus(expense.getPaymentStatus());
        dto.setExpensePaidBy(expense.getExpensePaidBy());
        dto.setApprovedAmount(expense.getApprovedAmount());
        dto.setPaidAmount(expense.getPaidAmount());

        dto.setAccountPostingStatus(expense.getAccountPostingStatus());
        dto.setAccountVoucherId(expense.getAccountVoucherId());
        dto.setAccountVoucherNumber(expense.getAccountVoucherNumber());
        dto.setAccountPostedAt(expense.getAccountPostedAt());
        dto.setAccountPostingError(expense.getAccountPostingError());

        dto.setReceiptVoucherId(expense.getReceiptVoucherId());
        dto.setReceiptVoucherNumber(expense.getReceiptVoucherNumber());
        dto.setInitialJournalVoucherId(expense.getInitialJournalVoucherId());
        dto.setInitialJournalVoucherNumber(expense.getInitialJournalVoucherNumber());

        dto.setFundTransferPostingStatus(expense.getFundTransferPostingStatus());
        dto.setFundTransferFromBankLedgerId(expense.getFundTransferFromBankLedgerId());
        dto.setFundTransferFromBankName(expense.getFundTransferFromBankName());
        dto.setFundTransferToBankLedgerId(expense.getFundTransferToBankLedgerId());
        dto.setFundTransferToBankName(expense.getFundTransferToBankName());
        dto.setFundTransferAmount(expense.getFundTransferAmount());
        dto.setFundTransferDate(expense.getFundTransferDate());
        dto.setFundTransferReference(expense.getFundTransferReference());
        dto.setFundTransferProofUrl(expense.getFundTransferProofUrl());
        dto.setFundTransferVoucherId(expense.getFundTransferVoucherId());
        dto.setFundTransferVoucherNumber(expense.getFundTransferVoucherNumber());
        dto.setFundTransferPostingError(expense.getFundTransferPostingError());

        dto.setPaymentBankLedgerId(expense.getPaymentBankLedgerId());
        dto.setPaymentBankName(expense.getPaymentBankName());

        return dto;
    }
}
