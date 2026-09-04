package com.doc.controller.project;

import com.doc.dto.project.portal.ProjectPortalApprovalQueueResponseDto;
import com.doc.dto.project.portal.ProjectPortalDetailApprovalDto;
import com.doc.dto.project.portal.ProjectPortalDetailListResponseDto;
import com.doc.dto.project.portal.ProjectPortalDetailRequestDto;
import com.doc.dto.project.portal.ProjectPortalDetailResponseDto;
import com.doc.entity.project.ProjectPortalDetailStatus;
import com.doc.service.project.ProjectPortalDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operationService/api/projects")
@Tag(
        name = "Project Portal Details",
        description = "Manage client portal login credentials "
                + "(EPR, FoSCoS, BIS, CTO, etc.)"
)
public class ProjectPortalDetailController {

    private final ProjectPortalDetailService
            projectPortalDetailService;

    public ProjectPortalDetailController(
            ProjectPortalDetailService projectPortalDetailService
    ) {
        this.projectPortalDetailService =
                projectPortalDetailService;
    }

    @Operation(
            summary = "Add client portal login details",
            description = "Technical department users assigned to the "
                    + "project can submit portal credentials"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Portal details saved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized or not assigned"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Project not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Portal already added"
            )
    })
    @PostMapping("/{projectId}/portal-details")
    public ResponseEntity<ProjectPortalDetailResponseDto>
    addPortalDetail(
            @PathVariable
            @Parameter(description = "Project ID")
            Long projectId,

            @RequestParam
            @Parameter(description = "Logged-in user ID")
            Long userId,

            @Valid
            @RequestBody
            ProjectPortalDetailRequestDto requestDto
    ) {
        ProjectPortalDetailResponseDto response =
                projectPortalDetailService.addPortalDetail(
                        projectId,
                        userId,
                        requestDto
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Get portal details for a project"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Portal details retrieved"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Unauthorized access"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Project not found"
            )
    })
    @GetMapping("/{projectId}/portal-details")
    public ResponseEntity<ProjectPortalDetailListResponseDto>
    getPortalDetails(
            @PathVariable
            @Parameter(description = "Project ID")
            Long projectId,

            @RequestParam
            @Parameter(description = "Logged-in user ID")
            Long userId
    ) {
        ProjectPortalDetailListResponseDto response =
                projectPortalDetailService.getPortalDetails(
                        projectId,
                        userId
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get portal-detail approval queue",
            description = "Admin and Operation Head see all Technical-user "
                    + "requests. A Technical manager sees requests submitted "
                    + "only by directly reporting Technical users."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Approval queue retrieved"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid status"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User cannot access approval queue"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    @GetMapping("/portal-details/approval-queue")
    public ResponseEntity<ProjectPortalApprovalQueueResponseDto>
    getApprovalQueue(
            @RequestParam
            @Parameter(description = "Logged-in manager/Admin/Operation Head ID")
            Long userId,

            @RequestParam(
                    required = false,
                    defaultValue = "PENDING"
            )
            @Parameter(
                    description = "Portal status: PENDING, APPROVED or REJECTED"
            )
            ProjectPortalDetailStatus status
    ) {
        ProjectPortalApprovalQueueResponseDto response =
                projectPortalDetailService.getApprovalQueue(
                        userId,
                        status
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update existing portal login details"
    )
    @PutMapping("/{projectId}/portal-details/{detailId}")
    public ResponseEntity<ProjectPortalDetailResponseDto>
    updatePortalDetail(
            @PathVariable Long projectId,
            @PathVariable Long detailId,
            @RequestParam Long userId,
            @Valid
            @RequestBody
            ProjectPortalDetailRequestDto requestDto
    ) {
        ProjectPortalDetailResponseDto response =
                projectPortalDetailService.updatePortalDetail(
                        projectId,
                        detailId,
                        userId,
                        requestDto
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Soft delete portal login details"
    )
    @DeleteMapping("/{projectId}/portal-details/{detailId}")
    public ResponseEntity<Void> deletePortalDetail(
            @PathVariable Long projectId,
            @PathVariable Long detailId,
            @RequestParam Long userId
    ) {
        projectPortalDetailService.deletePortalDetail(
                projectId,
                detailId,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Approve or reject portal details",
            description = "The submitter's Technical manager, Admin, "
                    + "or Operation Head can process a pending request"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Approval status updated"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid status or request is not pending"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Unauthorized approver"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Portal detail not found"
            )
    })
    @PutMapping(
            "/{projectId}/portal-details/{detailId}/approve"
    )
    public ResponseEntity<ProjectPortalDetailResponseDto>
    approveOrRejectPortalDetail(
            @PathVariable Long projectId,
            @PathVariable Long detailId,
            @RequestParam Long userId,
            @Valid
            @RequestBody
            ProjectPortalDetailApprovalDto approvalDto
    ) {
        ProjectPortalDetailResponseDto response =
                projectPortalDetailService
                        .approveOrRejectPortalDetail(
                                projectId,
                                detailId,
                                userId,
                                approvalDto
                        );

        return ResponseEntity.ok(response);
    }
}