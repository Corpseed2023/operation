package com.doc.controller.project;

import com.doc.dto.ProjectMilestoneassignment.ReassignMilestoneDto;
import com.doc.dto.ProjectMilestoneassignment.UpdateMilestoneStatusDto;
import com.doc.service.ProjectMilestoneAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/milestone-assignments")
public class ProjectMilestoneAssignmentController {

    @Autowired
    private ProjectMilestoneAssignmentService projectMilestoneAssignmentService;

    @Operation(summary = "Update the status of a project milestone assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Milestone status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or status transition"),
            @ApiResponse(responseCode = "404", description = "Milestone assignment or user not found")
    })
    @PutMapping("/{assignmentId}/status")
    public ResponseEntity<Void> updateMilestoneStatus(
            @PathVariable Long assignmentId,
            @Valid @RequestBody UpdateMilestoneStatusDto updateDto) {
        updateDto.setAssignmentId(assignmentId);
        projectMilestoneAssignmentService.updateMilestoneStatus(updateDto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reassign a project milestone to a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Milestone reassigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or ineligible user"),
            @ApiResponse(responseCode = "404", description = "Milestone assignment or user not found")
    })
    @PutMapping("/{assignmentId}/reassign")
    public ResponseEntity<Void> reassignMilestone(
            @PathVariable Long assignmentId,
            @Valid @RequestBody ReassignMilestoneDto reassignDto) {
        reassignDto.setAssignmentId(assignmentId);
        projectMilestoneAssignmentService.reassignMilestone(reassignDto);
        return ResponseEntity.ok().build();
    }

}