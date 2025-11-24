package com.doc.controller.project;

import com.doc.dto.project.AssignedMilestoneDto;
import com.doc.dto.project.AssignedProjectResponseDto;
import com.doc.dto.project.ProjectMilestoneResponseDto;
import com.doc.dto.project.ProjectRequestDto;
import com.doc.dto.project.ProjectResponseDto;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Operation(summary = "Create a new project")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Project number already exists"),
            @ApiResponse(responseCode = "404", description = "Referenced entity not found")
    })
    @PostMapping
    public ResponseEntity<ProjectResponseDto> createProject(@Valid @RequestBody ProjectRequestDto requestDto) {
        ProjectResponseDto response = projectService.createProject(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @GetMapping
    @Operation(summary = "Get all projects with pagination - hides projects for regular users if no visible milestone")
    public ResponseEntity<List<ProjectResponseDto>> getAllProjects(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        List<ProjectResponseDto> responses = projectService.getAllProjects(userId, page - 1, size);
        return ResponseEntity.ok(responses);
    }


    // 2. NEW: Separate count endpoint
    @GetMapping("/count")
    @Operation(summary = "Get total count of projects for user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total count retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Long> getProjectCount(@RequestParam Long userId) {
        long count = projectService.getProjectCount(userId);
        return ResponseEntity.ok(count);
    }
    @Operation(summary = "Delete a project by ID (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<ProjectResponseDto> addPaymentTransaction(
            @PathVariable Long id,
            @Valid @RequestBody ProjectPaymentTransactionDto transactionDto) {
        ProjectResponseDto response = projectService.addPaymentTransaction(id, transactionDto);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get assigned projects with milestones for the logged-in user with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged list of assigned projects retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/my-projects")
    public ResponseEntity<Page<AssignedProjectResponseDto>> getAssignedProjects(
            @Parameter(description = "User ID of the logged-in user") @RequestParam Long userId,
            @Parameter(description = "Page number (1-based)", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of records per page", example = "10") @RequestParam(defaultValue = "10") int size) {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be at least 1");
        }
        Page<AssignedProjectResponseDto> projects = projectService.getAssignedProjects(userId, page - 1, size);
        return ResponseEntity.ok(projects);
    }

    @Operation(summary = "Get project details and milestones based on user role and assignment",
            description = "Retrieves project details and milestones for a project. Accessible only to users with ADMIN or OPERATION_HEAD roles, assigned users, or their managers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project details and milestones retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "User is not authorized to view project details"),
            @ApiResponse(responseCode = "404", description = "Project or user not found")
    })
    @GetMapping("/{projectId}/milestones")
    public ResponseEntity<ProjectMilestoneResponseDto> getProjectMilestones(
            @PathVariable @Parameter(description = "ID of the project") Long projectId,
            @RequestParam @Parameter(description = "User ID of the logged-in user") Long userId) {
        ProjectMilestoneResponseDto response = projectService.getProjectMilestones(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Add payment using unbilled number instead of project ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payment data"),
            @ApiResponse(responseCode = "404", description = "No active project with given unbilled number")
    })
    @PostMapping("/payments/unbilled/{unbilledNumber}")
    public ResponseEntity<ProjectResponseDto> addPaymentByUnbilledNumber(
            @PathVariable @Parameter(description = "Unbilled number of the project") String unbilledNumber,
            @Valid @RequestBody ProjectPaymentTransactionDto dto) {

        ProjectResponseDto response = projectService.addPaymentByUnbilledNumber(unbilledNumber, dto);
        return ResponseEntity.ok(response);
    }

}