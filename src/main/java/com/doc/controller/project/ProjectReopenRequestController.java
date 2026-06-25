package com.doc.controller.project;

import com.doc.dto.project.reopen.ProjectReopenCreateRequestDto;
import com.doc.dto.project.reopen.ProjectReopenDecisionDto;
import com.doc.dto.project.reopen.ProjectReopenRequestResponseDto;
import com.doc.service.ProjectReopenRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/project-reopen-requests")
public class ProjectReopenRequestController {

    private final ProjectReopenRequestService projectReopenRequestService;

    public ProjectReopenRequestController(ProjectReopenRequestService projectReopenRequestService) {
        this.projectReopenRequestService = projectReopenRequestService;
    }

    @Operation(summary = "Create project reopen request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reopen request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid reopen request"),
            @ApiResponse(responseCode = "404", description = "Project, milestone assignment, or user not found")
    })
    @PostMapping
    public ResponseEntity<ProjectReopenRequestResponseDto> createReopenRequest(
            @Valid @RequestBody ProjectReopenCreateRequestDto dto
    ) {
        ProjectReopenRequestResponseDto response =
                projectReopenRequestService.createReopenRequest(dto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Requester manager approve or reject project reopen request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Requester manager decision saved"),
            @ApiResponse(responseCode = "400", description = "Invalid decision or request status"),
            @ApiResponse(responseCode = "404", description = "Reopen request or user not found")
    })
    @PutMapping("/{requestId}/requester-manager-decision")
    public ResponseEntity<ProjectReopenRequestResponseDto> requesterManagerDecision(
            @PathVariable Long requestId,
            @Valid @RequestBody ProjectReopenDecisionDto dto
    ) {
        ProjectReopenRequestResponseDto response =
                projectReopenRequestService.requesterManagerDecision(requestId, dto);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Responsible manager approve or reject project reopen request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Responsible manager decision saved"),
            @ApiResponse(responseCode = "400", description = "Invalid decision or request status"),
            @ApiResponse(responseCode = "404", description = "Reopen request or user not found")
    })
    @PutMapping("/{requestId}/responsible-manager-decision")
    public ResponseEntity<ProjectReopenRequestResponseDto> responsibleManagerDecision(
            @PathVariable Long requestId,
            @Valid @RequestBody ProjectReopenDecisionDto dto
    ) {
        ProjectReopenRequestResponseDto response =
                projectReopenRequestService.responsibleManagerDecision(requestId, dto);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get pending reopen requests for requester manager")
    @GetMapping("/pending/requester-manager/{managerId}")
    public ResponseEntity<List<ProjectReopenRequestResponseDto>> getRequesterManagerPendingRequests(
            @PathVariable Long managerId
    ) {
        List<ProjectReopenRequestResponseDto> response =
                projectReopenRequestService.getRequesterManagerPendingRequests(managerId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get pending reopen requests for responsible manager")
    @GetMapping("/pending/responsible-manager/{managerId}")
    public ResponseEntity<List<ProjectReopenRequestResponseDto>> getResponsibleManagerPendingRequests(
            @PathVariable Long managerId
    ) {
        List<ProjectReopenRequestResponseDto> response =
                projectReopenRequestService.getResponsibleManagerPendingRequests(managerId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get reopen request history for a project")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectReopenRequestResponseDto>> getProjectReopenRequests(
            @PathVariable Long projectId
    ) {
        List<ProjectReopenRequestResponseDto> response =
                projectReopenRequestService.getProjectReopenRequests(projectId);

        return ResponseEntity.ok(response);
    }

}