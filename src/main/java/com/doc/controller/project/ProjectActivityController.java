package com.doc.controller.project;

import com.doc.dto.project.activity.CreateCommentRequestDto;
import com.doc.dto.project.activity.expense.ApproveExpenseRequestDto;
import com.doc.dto.project.activity.expense.CreateExpenseRequestDto;
import com.doc.dto.project.activity.CreateNoteRequestDto;
import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;
import com.doc.em.ActivityType;
import com.doc.em.ApprovalStatus;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.service.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/operationService/api/projects/{projectId}/activities")
@RequiredArgsConstructor
public class ProjectActivityController {

    private final ProjectActivityService activityService;

    /**
     * Add project note
     */
    @PostMapping("/notes")
    public ResponseEntity<?> addNote(
            @PathVariable Long projectId,
            @RequestBody CreateNoteRequestDto request) {

        try {

            ProjectActivityResponseDto response = activityService.addNote(projectId, request);

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    /**
     * Add project comment
     */
    @PostMapping("/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long projectId,
            @RequestBody CreateCommentRequestDto request) {

        try {

            ProjectActivityResponseDto response = activityService.addComment(projectId, request);

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    /**
     * Add project expense
     */
    @PostMapping("/createExpenses")
    public ResponseEntity<?> addExpense(
            @PathVariable Long projectId,
            @RequestBody CreateExpenseRequestDto request) {

        try {
            ProjectActivityResponseDto response = activityService.addExpense(projectId, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (ResourceNotFoundException | ValidationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to create expense: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<?> getActivities(
            @PathVariable Long projectId,
            Pageable pageable) {

        try {

            Page<ProjectActivityResponseDto> response = activityService.getAllActivities(projectId, pageable);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    /**
     * Fetch activities by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<?> getActivitiesByType(
            @PathVariable Long projectId,
            @PathVariable ActivityType type,
            Pageable pageable) {

        try {

            Page<ProjectActivityResponseDto> response = activityService.getActivitiesByType(projectId, type, pageable);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    /**
     * Fetch activities by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<?> getActivitiesByDate(
            @PathVariable Long projectId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Pageable pageable) {

        try {

            Page<ProjectActivityResponseDto> response = activityService.getActivitiesByDateRange(projectId, startDate, endDate, pageable);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }


    /**
     * Approve / Reject / On-Hold an expense
     */
    @PutMapping("/approveExpense/{userId}/{expenseId}")
    public ResponseEntity<?> approveExpense(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @PathVariable Long expenseId,
            @RequestBody ApproveExpenseRequestDto request) {

        try {
            activityService.approveExpense(projectId, userId, expenseId, request);

            String message = "Expense " + request.getStatus().toUpperCase() + " successfully";
            return new ResponseEntity<>(message, HttpStatus.OK);

        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * Get list of expenses for a specific project (without pagination)
     */
    @GetMapping("/getProjectExpenses")
    public ResponseEntity<?> getProjectExpenses(
            @PathVariable Long projectId,
            @RequestParam Long userId) {     // Removed Pageable

        try {
            List<ProjectExpenseResponseDto> response =
                    activityService.getExpensesByProject(projectId, userId);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get paginated list of expenses with optional approval status filter
     * If approvalStatus=ALL or not provided → returns all expenses
     */
    /**
     * Get list of expenses for a user with optional approval status filter
     * Removed pagination as requested
     */
    @GetMapping("/getExpensesList")
    public ResponseEntity<?> getProjectExpenses(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "PENDING") ApprovalStatus approvalStatus) {

        try {
            List<ProjectExpenseResponseDto> response =
                    activityService.getExpenseList(userId, approvalStatus);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}