package com.doc.controller.project;

import com.doc.dto.project.activity.CreateCommentRequestDto;
import com.doc.dto.project.activity.CreateNoteRequestDto;
import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.em.ActivityType;
import com.doc.service.project.ProjectActivityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/operationService/api/projects/{projectId}/activities")
@RequiredArgsConstructor
@Validated
public class ProjectActivityController {

    private final ProjectActivityService activityService;

    @PostMapping("/notes")
    public ResponseEntity<ProjectActivityResponseDto> addNote(
            @PathVariable
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @Valid
            @RequestBody
            CreateNoteRequestDto request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(activityService.addNote(projectId, request));
    }

    @PostMapping("/comments")
    public ResponseEntity<ProjectActivityResponseDto> addComment(
            @PathVariable
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @Valid
            @RequestBody
            CreateCommentRequestDto request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(activityService.addComment(projectId, request));
    }

    /*
     * Expense creation is intentionally not present here.
     * Use ProjectExpenseController:
     * POST /operationService/api/projects/expenses?projectId={projectId}
     */

    @GetMapping
    public ResponseEntity<Page<ProjectActivityResponseDto>> getActivities(
            @PathVariable
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            Pageable pageable
    ) {

        return ResponseEntity.ok(
                activityService.getAllActivities(
                        projectId,
                        pageable
                )
        );
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<Page<ProjectActivityResponseDto>> getActivitiesByType(
            @PathVariable
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @PathVariable
            ActivityType type,

            Pageable pageable
    ) {

        return ResponseEntity.ok(
                activityService.getActivitiesByType(
                        projectId,
                        type,
                        pageable
                )
        );
    }

    @GetMapping("/date-range")
    public ResponseEntity<Page<ProjectActivityResponseDto>> getActivitiesByDate(
            @PathVariable
            @Positive(message = "Project ID must be greater than zero")
            Long projectId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            Pageable pageable
    ) {

        return ResponseEntity.ok(
                activityService.getActivitiesByDateRange(
                        projectId,
                        startDate,
                        endDate,
                        pageable
                )
        );
    }
}
