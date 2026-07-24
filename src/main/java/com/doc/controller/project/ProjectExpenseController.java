package com.doc.controller.project;

import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.dto.project.activity.expense.AccountsExpenseDecisionRequestDto;
import com.doc.dto.project.activity.expense.CreateExpenseRequestDto;
import com.doc.dto.project.activity.expense.CrtExpenseDecisionRequestDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;
import com.doc.em.ApprovalStatus;
import com.doc.em.ExpenseApprovalStage;
import com.doc.em.ExpensePaymentStatus;
import com.doc.service.ProjectActivityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Project expense management APIs.
 *
 * Workflow:
 *
 * Technical/Originating Department
 *          ↓
 * CRT_REVIEW
 *          ↓
 * ACCOUNTS_REVIEW
 *          ↓
 * APPROVED / PAYMENT PENDING
 */
@RestController
@RequestMapping("/operationService/api/projects/expenses")
@RequiredArgsConstructor
@Validated
public class ProjectExpenseController {

    private final ProjectActivityService activityService;

    /**
     * Create expense request.
     *
     * POST:
     * /operationService/api/projects/expenses?projectId=123
     */
    @PostMapping
    public ResponseEntity<ProjectActivityResponseDto> createExpense(
            @RequestParam
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @Valid
            @RequestBody
            CreateExpenseRequestDto request
    ) {

        ProjectActivityResponseDto response =
                activityService.addExpense(projectId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * CRT approves, rejects or places an expense on hold.
     *
     * PUT:
     * /operationService/api/projects/expenses/{expenseId}/crt-decision
     * ?projectId=123&userId=456
     */
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

        ProjectExpenseResponseDto response =
                activityService.takeCrtExpenseDecision(
                        projectId,
                        expenseId,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Accounts approves, rejects or places an expense on hold.
     *
     * PUT:
     * /operationService/api/projects/expenses/{expenseId}/accounts-decision
     * ?projectId=123&userId=456
     */
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

        ProjectExpenseResponseDto response =
                activityService.takeAccountsExpenseDecision(
                        projectId,
                        expenseId,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Get expenses requiring action at a specific approval stage.
     *
     * CRT queue:
     * GET /operationService/api/projects/expenses/approval-queue
     * ?userId=10&approvalStage=CRT_REVIEW&approvalStatus=PENDING
     *
     * Accounts queue:
     * GET /operationService/api/projects/expenses/approval-queue
     * ?userId=20&approvalStage=ACCOUNTS_REVIEW&approvalStatus=PENDING
     */
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

        List<ProjectExpenseResponseDto> response =
                activityService.getExpenseApprovalQueue(
                        userId,
                        approvalStage,
                        approvalStatus
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Get payment queue.
     *
     * Example:
     * GET /operationService/api/projects/expenses/payment-queue
     * ?userId=20&paymentStatus=PENDING
     */
    @GetMapping("/payment-queue")
    public ResponseEntity<List<ProjectExpenseResponseDto>> getPaymentQueue(
            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId,

            @RequestParam(required = false)
            ExpensePaymentStatus paymentStatus
    ) {

        List<ProjectExpenseResponseDto> response =
                activityService.getExpensePaymentQueue(
                        userId,
                        paymentStatus
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Get all project expenses.
     *
     * GET:
     * /operationService/api/projects/expenses/project/123?userId=456
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectExpenseResponseDto>> getProjectExpenses(
            @PathVariable
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId
    ) {

        List<ProjectExpenseResponseDto> response =
                activityService.getExpensesByProject(
                        projectId,
                        userId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Get a single expense.
     *
     * GET:
     * /operationService/api/projects/expenses/789?userId=456
     */
    @GetMapping("/{expenseId}")
    public ResponseEntity<ProjectExpenseResponseDto> getExpenseById(
            @PathVariable
            @Positive(message = "Expense ID must be greater than zero")
            Long expenseId,

            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId
    ) {

        ProjectExpenseResponseDto response =
                activityService.getExpenseById(expenseId, userId);

        return ResponseEntity.ok(response);
    }
}