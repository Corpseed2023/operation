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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/projects/{projectId}/activities")
@RequiredArgsConstructor
public class ProjectActivityController {

    private final ProjectActivityService activityService;

    /**
     * Add project note
     */
    @PostMapping("/notes")
    public ProjectActivityResponseDto addNote(
            @PathVariable Long projectId,
            @RequestBody CreateNoteRequestDto request) {

        return activityService.addNote(projectId, request);
    }

    /**
     * Add project comment
     */
    @PostMapping("/comments")
    public ProjectActivityResponseDto addComment(
            @PathVariable Long projectId,
            @RequestBody CreateCommentRequestDto request) {

        return activityService.addComment(projectId, request);
    }

    /**
     * Add project expense
     */
    @PostMapping("/expenses")
    public ProjectActivityResponseDto addExpense(
            @PathVariable Long projectId,
            @RequestBody CreateExpenseRequestDto request) {

        return activityService.addExpense(projectId, request);
    }

    @GetMapping
    public Page<ProjectActivityResponseDto> getActivities(
            @PathVariable Long projectId,
            Pageable pageable) {

        return activityService.getAllActivities(projectId, pageable);
    }

    /**
     * Fetch activities by type
     */
    @GetMapping("/type/{type}")
    public Page<ProjectActivityResponseDto> getActivitiesByType(
            @PathVariable Long projectId,
            @PathVariable ActivityType type,
            Pageable pageable) {

        return activityService.getActivitiesByType(projectId, type, pageable);
    }

    /**
     * Fetch activities by date range
     */
    @GetMapping("/date-range")
    public Page<ProjectActivityResponseDto> getActivitiesByDate(
            @PathVariable Long projectId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Pageable pageable) {

        return activityService.getActivitiesByDateRange(projectId, startDate, endDate, pageable);
    }

}
