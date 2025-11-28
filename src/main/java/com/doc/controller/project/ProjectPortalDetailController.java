package com.doc.controller.project;



import com.doc.dto.project.portal.ProjectPortalDetailListResponseDto;
import com.doc.dto.project.portal.ProjectPortalDetailRequestDto;
import com.doc.dto.project.portal.ProjectPortalDetailResponseDto;
import com.doc.service.ProjectPortalDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Project Portal Details", description = "Manage client portal login credentials (EPR, FoSCoS, BIS, CTO, etc.)")
public class ProjectPortalDetailController {

    private final ProjectPortalDetailService projectPortalDetailService;

    public ProjectPortalDetailController(ProjectPortalDetailService projectPortalDetailService) {
        this.projectPortalDetailService = projectPortalDetailService;
    }


    @Operation(summary = "Add client portal login details",
            description = "Used by CRT/Operations team to save government portal credentials (e.g., CPCB EPR, FoSCoS, Parivesh)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Portal details saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Not assigned to this project"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "409", description = "Portal already added")
    })
    @PostMapping("/{projectId}/portal-details")
    public ResponseEntity<ProjectPortalDetailResponseDto> addPortalDetail(
            @PathVariable @Parameter(description = "Project ID") Long projectId,
            @RequestParam @Parameter(description = "Logged-in user ID") Long userId,
            @Valid @RequestBody ProjectPortalDetailRequestDto requestDto) {

        ProjectPortalDetailResponseDto response = projectPortalDetailService.addPortalDetail(projectId, userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all portal login details for a project")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List retrieved"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/{projectId}/portal-details")
    public ResponseEntity<ProjectPortalDetailListResponseDto> getPortalDetails(
            @PathVariable @Parameter(description = "Project ID") Long projectId,
            @RequestParam @Parameter(description = "Logged-in user ID") Long userId) {

        ProjectPortalDetailListResponseDto response = projectPortalDetailService.getPortalDetails(projectId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update existing portal login details")
    @PutMapping("/{projectId}/portal-details/{detailId}")
    public ResponseEntity<ProjectPortalDetailResponseDto> updatePortalDetail(
            @PathVariable Long projectId,
            @PathVariable Long detailId,
            @RequestParam Long userId,
            @Valid @RequestBody ProjectPortalDetailRequestDto requestDto) {

        ProjectPortalDetailResponseDto response = projectPortalDetailService.updatePortalDetail(projectId, detailId, userId, requestDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete (soft delete) portal login details")
    @DeleteMapping("/{projectId}/portal-details/{detailId}")
    public ResponseEntity<Void> deletePortalDetail(
            @PathVariable Long projectId,
            @PathVariable Long detailId,
            @RequestParam Long userId) {

        projectPortalDetailService.deletePortalDetail(projectId, detailId, userId);
        return ResponseEntity.noContent().build();
    }
}