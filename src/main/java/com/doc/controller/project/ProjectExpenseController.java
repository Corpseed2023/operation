package com.doc.controller.project;

import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.dto.project.activity.expense.*;
import com.doc.em.ApprovalStatus;
import com.doc.em.ExpenseApprovalStage;
import com.doc.em.ExpensePaymentStatus;
import com.doc.service.ProjectActivityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/operationService/api/projects/expenses")
@RequiredArgsConstructor
@Validated
public class ProjectExpenseController {

    private final ProjectActivityService activityService;

    @PostMapping
    public ResponseEntity<ProjectActivityResponseDto> createExpense(
            @RequestParam
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @Valid
            @RequestBody
            CreateExpenseRequestDto request
    ) {

        log.info(
                "[EXPENSE-CREATE-REQUEST] projectId={} | createdByUserId={} | departmentId={} | category={}",
                projectId,
                request.getCreatedByUserId(),
                request.getDepartmentId(),
                request.getExpenseCategory()
        );

        ProjectActivityResponseDto response =
                activityService.addExpense(projectId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{expenseId}/crt-decision")
    public ResponseEntity<ProjectExpenseResponseDto> takeCrtDecision(
            @PathVariable
            @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,

            @RequestParam
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId,

            @Valid
            @RequestBody
            CrtExpenseDecisionRequestDto request
    ) {

        log.info(
                "[CRT-DECISION-REQUEST] projectId={} | expenseId={} | userId={} | decision={} | expensePaidBy={} | paymentMode={} | bankLedgerId={}",
                projectId,
                expenseId,
                userId,
                request.getStatus(),
                request.getExpensePaidBy(),
                request.getClientPaymentMode(),
                request.getClientPaymentBankLedgerId()
        );

        ProjectExpenseResponseDto response =
                activityService.takeCrtExpenseDecision(
                        projectId,
                        expenseId,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }
    @PutMapping("/{expenseId}/accounts-decision")
    public ResponseEntity<ProjectExpenseResponseDto> takeAccountsDecision(
            @PathVariable
            @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,

            @RequestParam
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId,

            @Valid
            @RequestBody
            AccountsExpenseDecisionRequestDto request
    ) {

        log.info(
                "[ACCOUNTS-DECISION-REQUEST] projectId={} | expenseId={} | userId={} | decision={} | approvedAmount={}",
                projectId,
                expenseId,
                userId,
                request.getStatus(),
                request.getApprovedAmount()
        );

        ProjectExpenseResponseDto response =
                activityService.takeAccountsExpenseDecision(
                        projectId,
                        expenseId,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{expenseId}/fund-transfer")
    public ResponseEntity<ProjectExpenseResponseDto>
    transferGovernmentFeeFunds(
            @PathVariable
            @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,

            @RequestParam
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId,

            @Valid
            @RequestBody
            GovernmentFeeFundTransferRequestDto request
    ) {

        log.info(
                "[GOVERNMENT-FEE-FUND-TRANSFER] projectId={} | expenseId={} | userId={} | fromLedgerId={} | toLedgerId={} | amount={}",
                projectId,
                expenseId,
                userId,
                request.getFromBankLedgerId(),
                request.getToBankLedgerId(),
                request.getAmount()
        );

        return ResponseEntity.ok(
                activityService.transferGovernmentFeeFunds(
                        projectId,
                        expenseId,
                        userId,
                        request
                )
        );
    }


    @GetMapping("/approval-queue")
    public ResponseEntity<List<ProjectExpenseResponseDto>> getApprovalQueue(
            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId,

            @RequestParam
            ExpenseApprovalStage approvalStage,

            @RequestParam(required = false)
            ApprovalStatus approvalStatus
    ) {

        return ResponseEntity.ok(
                activityService.getExpenseApprovalQueue(
                        userId,
                        approvalStage,
                        approvalStatus
                )
        );
    }

    @GetMapping("/payment-queue")
    public ResponseEntity<List<ProjectExpenseResponseDto>> getPaymentQueue(
            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId,

            @RequestParam(required = false)
            ExpensePaymentStatus paymentStatus
    ) {

        return ResponseEntity.ok(
                activityService.getExpensePaymentQueue(
                        userId,
                        paymentStatus
                )
        );
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectExpenseResponseDto>> getProjectExpenses(
            @PathVariable
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId
    ) {

        return ResponseEntity.ok(
                activityService.getExpensesByProject(
                        projectId,
                        userId
                )
        );
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ProjectExpenseResponseDto> getExpenseById(
            @PathVariable
            @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,

            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId
    ) {

        return ResponseEntity.ok(
                activityService.getExpenseById(
                        expenseId,
                        userId
                )
        );
    }
}
