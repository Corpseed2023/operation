package com.doc.controller.project;

import com.doc.dto.document.DocumentChecklistDTO;
import com.doc.dto.project.AssignedProjectResponseDto;
import com.doc.dto.project.ProjectMilestoneResponseDto;
import com.doc.dto.project.ProjectRequestDto;
import com.doc.dto.project.ProjectResponseDto;
import com.doc.dto.project.projectHistory.MilestoneHistoryResponseDto;
import com.doc.dto.project.projectHistory.ProjectHistoryResponseDto;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.service.ProjectSearchService;
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
import java.util.Map;

@RestController
@RequestMapping("/operationService/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectSearchService projectSearchService;


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


    @GetMapping("/count")
    @Operation(summary = "Get total count of searched projects")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total count retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Long> getProjectCount(
            @RequestParam Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String value) {

        // If no search filter is provided → return total project count
        if (type == null || value == null) {
            long totalCount = projectService.getProjectCount(userId);
            return ResponseEntity.ok(totalCount);
        }

        // Search-based counting
        List<ProjectResponseDto> projects;

        switch (type.toLowerCase()) {
            case "company":
                projects = projectSearchService.searchProjectsByCompanyName(value, userId);
                break;

            case "projectnumber":
                projects = projectSearchService.searchProjectsByProjectNumber(value, userId);
                break;

            case "contact":
                projects = projectSearchService.searchProjectsByContactName(value, userId);
                break;

            case "projectname":
                projects = projectSearchService.searchProjectsByProjectName(value, userId);
                break;

            default:
                return ResponseEntity.badRequest().body(0L);
        }

        return ResponseEntity.ok((long) projects.size());
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


    @Operation(summary = "Get project history including creation, milestones, assignments, and status changes",
            description = "Retrieves detailed history for a project, including when it was created, its milestones (starting with the first), assignments (to whom and by whom), and status changes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/{projectId}/history")
    public ResponseEntity<ProjectHistoryResponseDto> getProjectHistory(
            @PathVariable @Parameter(description = "ID of the project") Long projectId) {
        ProjectHistoryResponseDto response = projectService.getProjectHistory(projectId);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get complete history of a specific milestone in a project",
            description = "Returns assignment and status change history of one milestone. Accessible to assigned user, their manager, Admin, or Operation Head.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Milestone history retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized to view this milestone history"),
            @ApiResponse(responseCode = "404", description = "Project or milestone not found")
    })
    @GetMapping("/{projectId}/milestones/{milestoneId}/history")
    public ResponseEntity<MilestoneHistoryResponseDto> getMilestoneHistory(
            @PathVariable Long projectId,
            @PathVariable Long milestoneId,
            @RequestParam Long userId) {

        MilestoneHistoryResponseDto response = projectService.getMilestoneHistory(projectId, milestoneId, userId);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{projectId}/applicant-type")
    @Operation(summary = "CRT selects Applicant Type (Importer, Brand Owner, etc.)")
    public ResponseEntity<Void> setApplicantType(
            @PathVariable Long projectId,
            @RequestParam("applicantTypeId") Long applicantTypeId) {


        projectService.setApplicantType(projectId, applicantTypeId);
        return ResponseEntity.ok().build();
    }



    @GetMapping("/{projectId}/document-checklist")
    @Operation(summary = "Get project document checklist (Applicant Type based) — visible to CRT, Legal, Technical, Admin")
    public ResponseEntity<List<DocumentChecklistDTO>> getDocumentChecklist(
            @PathVariable Long projectId,
            @RequestParam Long userId) {

        List<DocumentChecklistDTO> checklist = projectService.getDocumentChecklist(projectId);
        return ResponseEntity.ok(checklist);
    }


    @GetMapping("/{unbilledNumber}")
    public ResponseEntity<ProjectResponseDto> getProjectByUnbilledNumber(
            @PathVariable String unbilledNumber) {

        ProjectResponseDto response = projectService.getProjectByUnbilledNumber(unbilledNumber);
        return ResponseEntity.ok(response);
    }


}