package com.doc.controller.project;

import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.dto.project.activity.expense.AccountsExpenseDecisionRequestDto;
import com.doc.dto.project.activity.expense.CreateExpenseRequestDto;
import com.doc.dto.project.activity.expense.CrtExpenseDecisionRequestDto;
import com.doc.dto.project.activity.expense.GovernmentFeeFundTransferRequestDto;
import com.doc.dto.project.activity.expense.GovernmentFeePaymentRequestDto;
import com.doc.dto.project.activity.expense.GovernmentFeePaymentDecisionRequestDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;
import com.doc.em.ApprovalStatus;
import com.doc.em.ExpenseApprovalStage;
import com.doc.em.ExpensePaymentStatus;
import com.doc.service.project.ProjectActivityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam @Positive(message = "Project ID must be greater than zero")
            Long projectId,
            @Valid @RequestBody CreateExpenseRequestDto request
    ) {
        log.info(
                "[EXPENSE-CREATE-REQUEST] projectId={} | createdByUserId={} | departmentId={} | category={}",
                projectId,
                request.getCreatedByUserId(),
                request.getDepartmentId(),
                request.getExpenseCategory()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.addExpense(projectId, request));
    }

    @PutMapping("/{expenseId}/crt-decision")
    public ResponseEntity<ProjectExpenseResponseDto> takeCrtDecision(
            @PathVariable @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,
            @RequestParam @Positive(message = "Project ID must be greater than zero")
            Long projectId,
            @RequestParam @Positive(message = "User ID must be greater than zero")
            Long userId,
            @Valid @RequestBody CrtExpenseDecisionRequestDto request
    ) {
        return ResponseEntity.ok(
                activityService.takeCrtExpenseDecision(
                        projectId,
                        expenseId,
                        userId,
                        request
                )
        );
    }

    @PutMapping("/{expenseId}/accounts-decision")
    public ResponseEntity<ProjectExpenseResponseDto> takeAccountsDecision(
            @PathVariable @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,
            @RequestParam @Positive(message = "Project ID must be greater than zero")
            Long projectId,
            @RequestParam @Positive(message = "User ID must be greater than zero")
            Long userId,
            @Valid @RequestBody AccountsExpenseDecisionRequestDto request
    ) {
        return ResponseEntity.ok(
                activityService.takeAccountsExpenseDecision(
                        projectId,
                        expenseId,
                        userId,
                        request
                )
        );
    }

    /** Step 4: HDFC/Kotak to Axis CONTRA voucher. */
    @PostMapping("/{expenseId}/fund-transfer")
    public ResponseEntity<ProjectExpenseResponseDto>
    transferGovernmentFeeFunds(
            @PathVariable @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,
            @RequestParam @Positive(message = "Project ID must be greater than zero")
            Long projectId,
            @RequestParam @Positive(message = "User ID must be greater than zero")
            Long userId,
            @Valid @RequestBody GovernmentFeeFundTransferRequestDto request
    ) {
        return ResponseEntity.ok(
                activityService.transferGovernmentFeeFunds(
                        projectId,
                        expenseId,
                        userId,
                        request
                )
        );
    }

    /** Step 5A: Technical submits portal-payment details and receipt. */
    @PutMapping("/{expenseId}/government-fee/payment-proof")
    public ResponseEntity<ProjectExpenseResponseDto>
    submitGovernmentFeePaymentProof(
            @PathVariable @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,
            @RequestParam @Positive(message = "Project ID must be greater than zero")
            Long projectId,
            @RequestParam @Positive(message = "User ID must be greater than zero")
            Long userId,
            @Valid @RequestBody GovernmentFeePaymentRequestDto request
    ) {
        log.info(
                "[GOVERNMENT-FEE-PAYMENT-PROOF-REQUEST] projectId={} | expenseId={} | userId={} | amount={} | paymentDate={} | reference={}",
                projectId,
                expenseId,
                userId,
                request.getAmount(),
                request.getPaymentDate(),
                request.getPaymentReference()
        );

        return ResponseEntity.ok(
                activityService.submitGovernmentFeePaymentProof(
                        projectId,
                        expenseId,
                        userId,
                        request
                )
        );
    }


    /**
     * Step 5B: Accounts approves or rejects Technical's payment proof. The
     * Account Service PAYMENT voucher is created only for APPROVED.
     */
    @PutMapping("/{expenseId}/government-fee/payment-decision")
    public ResponseEntity<ProjectExpenseResponseDto>
    takeGovernmentFeePaymentDecision(
            @PathVariable @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,
            @RequestParam @Positive(message = "Project ID must be greater than zero")
            Long projectId,
            @RequestParam @Positive(message = "User ID must be greater than zero")
            Long userId,
            @Valid @RequestBody GovernmentFeePaymentDecisionRequestDto request
    ) {
        return ResponseEntity.ok(
                activityService.takeGovernmentFeePaymentDecision(
                        projectId,
                        expenseId,
                        userId,
                        request
                )
        );
    }

    @GetMapping("/approval-queue")
    public ResponseEntity<List<ProjectExpenseResponseDto>> getApprovalQueue(
            @RequestParam @Positive(message = "User ID must be greater than zero")
            Long userId,
            @RequestParam ExpenseApprovalStage approvalStage,
            @RequestParam(required = false) ApprovalStatus approvalStatus
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
            @RequestParam @Positive(message = "User ID must be greater than zero")
            Long userId,
            @RequestParam(required = false) ExpensePaymentStatus paymentStatus
    ) {
        return ResponseEntity.ok(
                activityService.getExpensePaymentQueue(userId, paymentStatus)
        );
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectExpenseResponseDto>> getProjectExpenses(
            @PathVariable @Positive(message = "Project ID must be greater than zero")
            Long projectId,
            @RequestParam @Positive(message = "User ID must be greater than zero")
            Long userId
    ) {
        return ResponseEntity.ok(
                activityService.getExpensesByProject(projectId, userId)
        );
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ProjectExpenseResponseDto> getExpenseById(
            @PathVariable @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,
            @RequestParam @Positive(message = "User ID must be greater than zero")
            Long userId
    ) {
        return ResponseEntity.ok(
                activityService.getExpenseById(expenseId, userId)
        );
    }
}
