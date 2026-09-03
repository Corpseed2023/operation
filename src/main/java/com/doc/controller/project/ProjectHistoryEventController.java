package com.doc.controller.project;

import com.doc.dto.project.projectHistory.ProjectHistoryEventResponseDto;
import com.doc.em.ProjectHistoryEventType;
import com.doc.service.project.ProjectHistoryEventService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@Validated
@RequestMapping("/operationService/api/projects")
public class ProjectHistoryEventController {

    private final ProjectHistoryEventService historyEventService;

    public ProjectHistoryEventController(
            ProjectHistoryEventService historyEventService
    ) {
        this.historyEventService = historyEventService;
    }

    @GetMapping("/{projectId}/timeline")
    @Operation(
            summary = "Get complete project history timeline",
            description = "Returns complete history of the project including project, milestone, document, legal, procurement, payment and other events"
    )
    public ResponseEntity<List<ProjectHistoryEventResponseDto>>
    getProjectTimeline(
            @PathVariable
            @Positive(message = "Project ID must be greater than zero")
            Long projectId
    ) {

        return ResponseEntity.ok(
                historyEventService.getProjectTimeline(projectId)
        );
    }


}
