package com.doc.controller.project;

import com.doc.dto.project.activity.CreateCommentRequestDto;
import com.doc.dto.project.activity.CreateExpenseRequestDto;
import com.doc.dto.project.activity.CreateNoteRequestDto;
import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.em.ActivityType;
import com.doc.service.ProjectActivityService;
import com.doc.service.ProjectSearchService;
import com.doc.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
    @PostMapping("/expenses")
    public ResponseEntity<?> addExpense(
            @PathVariable Long projectId,
            @RequestBody CreateExpenseRequestDto request) {

        try {

            ProjectActivityResponseDto response = activityService.addExpense(projectId, request);

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

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


    @PutMapping("/approveExpense/{userId}/{expenseId}")
    public ResponseEntity<?> approveExpense(
            @PathVariable("projectId") Long projectId,
            @PathVariable("userId") Long userId,
            @PathVariable("expenseId") Long expenseId
    ){
        activityService.approveExpense(projectId, userId, expenseId);

        return new ResponseEntity<>("Expense approved successfully", HttpStatus.OK);
    }

}