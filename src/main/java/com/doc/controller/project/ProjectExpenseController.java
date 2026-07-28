

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

        log.debug(
                "[EXPENSE-CREATE-SERVICE-CALL] Calling addExpense | projectId={}",
                projectId
        );

        ProjectActivityResponseDto response =
                activityService.addExpense(projectId, request);

        log.info(
                "[EXPENSE-CREATE-SUCCESS] Expense request created | projectId={} | createdByUserId={}",
                projectId,
                request.getCreatedByUserId()
        );

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
                "[CRT-DECISION-REQUEST] projectId={} | expenseId={} | userId={} | decision={}",
                projectId,
                expenseId,
                userId,
                request.getStatus()
        );

        ProjectExpenseResponseDto response =
                activityService.takeCrtExpenseDecision(
                        projectId,
                        expenseId,
                        userId,
                        request
                );

        log.info(
                "[CRT-DECISION-SUCCESS] projectId={} | expenseId={} | userId={} | decision={}",
                projectId,
                expenseId,
                userId,
                request.getStatus()
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

        log.info(
                "[ACCOUNTS-DECISION-SUCCESS] projectId={} | expenseId={} | userId={} | decision={}",
                projectId,
                expenseId,
                userId,
                request.getStatus()
        );

        return ResponseEntity.ok(response);
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

        log.info(
                "[EXPENSE-APPROVAL-QUEUE-REQUEST] userId={} | approvalStage={} | approvalStatus={}",
                userId,
                approvalStage,
                approvalStatus
        );

        List<ProjectExpenseResponseDto> response =
                activityService.getExpenseApprovalQueue(
                        userId,
                        approvalStage,
                        approvalStatus
                );

        log.info(
                "[EXPENSE-APPROVAL-QUEUE-SUCCESS] userId={} | approvalStage={} | recordCount={}",
                userId,
                approvalStage,
                response.size()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment-queue")
    public ResponseEntity<List<ProjectExpenseResponseDto>> getPaymentQueue(
            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId,

            @RequestParam(required = false)
            ExpensePaymentStatus paymentStatus
    ) {

        log.info(
                "[EXPENSE-PAYMENT-QUEUE-REQUEST] userId={} | paymentStatus={}",
                userId,
                paymentStatus
        );

        List<ProjectExpenseResponseDto> response =
                activityService.getExpensePaymentQueue(
                        userId,
                        paymentStatus
                );

        log.info(
                "[EXPENSE-PAYMENT-QUEUE-SUCCESS] userId={} | paymentStatus={} | recordCount={}",
                userId,
                paymentStatus,
                response.size()
        );

        return ResponseEntity.ok(response);
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

        log.info(
                "[PROJECT-EXPENSE-LIST-REQUEST] projectId={} | userId={}",
                projectId,
                userId
        );

        List<ProjectExpenseResponseDto> response =
                activityService.getExpensesByProject(
                        projectId,
                        userId
                );

        log.info(
                "[PROJECT-EXPENSE-LIST-SUCCESS] projectId={} | recordCount={}",
                projectId,
                response.size()
        );

        return ResponseEntity.ok(response);
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

        log.info(
                "[EXPENSE-DETAIL-REQUEST] expenseId={} | userId={}",
                expenseId,
                userId
        );

        ProjectExpenseResponseDto response =
                activityService.getExpenseById(expenseId, userId);

        log.info(
                "[EXPENSE-DETAIL-SUCCESS] expenseId={} | userId={}",
                expenseId,
                userId
        );

        return ResponseEntity.ok(response);
    }

}