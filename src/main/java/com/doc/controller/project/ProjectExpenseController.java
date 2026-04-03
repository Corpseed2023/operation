package com.doc.controller.project;

import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.dto.project.activity.expense.ApproveExpenseRequestDto;
import com.doc.dto.project.activity.expense.CreateExpenseRequestDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;
import com.doc.em.ApprovalStatus;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.service.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dedicated Controller for Project Expense Management
 */
@RestController
@RequestMapping("/operationService/api/projects/expenses")
@RequiredArgsConstructor
public class ProjectExpenseController {

    private final ProjectActivityService activityService;

    /**
     * Create a new expense for a project
     * POST /operationService/api/projects/expenses?projectId=123
     */
    @PostMapping
    public ResponseEntity<?> createExpense(
            @RequestParam Long projectId,
            @RequestBody CreateExpenseRequestDto request) {

        try {
            ProjectActivityResponseDto response = activityService.addExpense(projectId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ResourceNotFoundException | ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to create expense: " + e.getMessage());
        }
    }

    /**
     * Approve / Reject / On-Hold an expense
     * PUT /operationService/api/projects/expenses/approve?projectId=123&userId=456&expenseId=789
     */
    @PutMapping("/approve")
    public ResponseEntity<?> approveExpense(
            @RequestParam Long projectId,
            @RequestParam Long userId,
            @RequestParam Long expenseId,
            @RequestBody ApproveExpenseRequestDto request) {

        try {
            activityService.approveExpense(projectId, userId, expenseId, request);

            String message = "Expense " + request.getStatus().toUpperCase() + " successfully";
            return ResponseEntity.ok(message);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Get expenses list for a user with optional approval status filter
     * GET /operationService/api/projects/expenses/getExpensesList?userId=123&approvalStatus=PENDING
     * Default approvalStatus = PENDING
     */
    @GetMapping("/getExpensesList")
    public ResponseEntity<?> getProjectExpensesList(
            @RequestParam Long userId,
            @RequestParam(required = false) String approvalStatus) {

        ApprovalStatus status = null;

        if (approvalStatus != null && !approvalStatus.equalsIgnoreCase("ALL")) {
            try {
                status = ApprovalStatus.valueOf(approvalStatus.toUpperCase());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid approvalStatus");
            }
        }

        System.out.println("Raw approvalStatus: " + approvalStatus);
        System.out.println("Mapped status: " + status);

        List<ProjectExpenseResponseDto> response =
                activityService.getExpenseList(userId, status);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all expenses for a specific project (filtered by viewing user)
     * GET /operationService/api/projects/expenses/getProjectExpenses?projectId=123&userId=456
     */
    @GetMapping("/getProjectExpenses")
    public ResponseEntity<?> getProjectExpenses(
            @RequestParam Long projectId,
            @RequestParam Long userId) {

        try {
            List<ProjectExpenseResponseDto> response =
                    activityService.getExpensesByProject(projectId, userId);

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Alternative clean endpoint (Recommended)
     * GET /operationService/api/projects/expenses?projectId=123&userId=456
     */
    @GetMapping
    public ResponseEntity<?> getProjectExpensesByProject(
            @RequestParam Long projectId,
            @RequestParam Long userId) {

        try {
            List<ProjectExpenseResponseDto> response =
                    activityService.getExpensesByProject(projectId, userId);

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}