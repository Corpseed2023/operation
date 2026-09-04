package com.doc.controller.project;

import com.doc.dto.ProjectMilestoneassignment.MilestoneAcknowledgementResponseDto;
import com.doc.dto.ProjectMilestoneassignment.ReassignMilestoneDto;
import com.doc.dto.ProjectMilestoneassignment.ReassignMilestoneResponseDto;
import com.doc.dto.ProjectMilestoneassignment.SendBackToPreviousMilestoneDto;
import com.doc.dto.ProjectMilestoneassignment.UpdateMilestoneStatusDto;
import com.doc.service.project.ProjectMilestoneAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/operationService/api/milestone-assignments"
)
public class ProjectMilestoneAssignmentController {

    private final ProjectMilestoneAssignmentService
            projectMilestoneAssignmentService;

    public ProjectMilestoneAssignmentController(
            ProjectMilestoneAssignmentService
                    projectMilestoneAssignmentService
    ) {
        this.projectMilestoneAssignmentService =
                projectMilestoneAssignmentService;
    }

    @PutMapping("/{assignmentId}/status")
    @Operation(
            summary = "Update project milestone assignment status"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Milestone status updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or status transition"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone assignment or user not found"
            )
    })
    public ResponseEntity<String> updateMilestoneStatus(
            @PathVariable Long assignmentId,
            @Valid
            @RequestBody UpdateMilestoneStatusDto updateDto
    ) {
        updateDto.setAssignmentId(assignmentId);

        projectMilestoneAssignmentService
                .updateMilestoneStatus(updateDto);

        return ResponseEntity.ok(
                "Milestone status updated successfully"
        );
    }


    @GetMapping(
            "/{currentAssignmentId}/previous-completion-acknowledgements"
    )
    @Operation(
            summary = "Get acknowledgements from previous completed milestones",
            description = """
                    Returns acknowledgements uploaded while completing
                    milestones positioned before the logged-in user's
                    current milestone.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Acknowledgements retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not authorized or milestone is not visible"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone assignment or user not found"
            )
    })
    public ResponseEntity<
            List<MilestoneAcknowledgementResponseDto>
            > getPreviousCompletionAcknowledgements(
            @PathVariable Long currentAssignmentId,
            @RequestParam Long userId
    ) {
        List<MilestoneAcknowledgementResponseDto> response =
                projectMilestoneAssignmentService
                        .getPreviousCompletionAcknowledgements(
                                currentAssignmentId,
                                userId
                        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{assignmentId}/reassign")
    @Operation(
            summary = "Manually reassign project milestone"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Milestone reassigned successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or ineligible user"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone assignment or user not found"
            )
    })
    public ResponseEntity<ReassignMilestoneResponseDto>
    reassignMilestone(
            @PathVariable Long assignmentId,
            @Valid
            @RequestBody ReassignMilestoneDto reassignDto
    ) {
        reassignDto.setAssignmentId(assignmentId);

        ReassignMilestoneResponseDto response =
                projectMilestoneAssignmentService
                        .reassignMilestone(reassignDto);

        return ResponseEntity.ok(response);
    }

    @PostMapping(
            "/{assignmentId}/send-back-to-previous"
    )
    @Operation(
            summary = "Send current milestone back to previous milestone"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Previous milestone moved to rework"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone assignment or user not found"
            )
    })
    public ResponseEntity<String>
    sendBackToPreviousMilestone(
            @PathVariable Long assignmentId,
            @Valid
            @RequestBody SendBackToPreviousMilestoneDto dto
    ) {
        dto.setCurrentAssignmentId(assignmentId);

        projectMilestoneAssignmentService
                .sendBackToPreviousMilestone(dto);

        return ResponseEntity.ok(
                "Milestone sent back to previous milestone for rework successfully"
        );
    }
}