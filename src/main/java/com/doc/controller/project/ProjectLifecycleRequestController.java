package com.doc.controller.project;

import com.doc.dto.project.lifecycle.CreateProjectLifecycleRequestDto;
import com.doc.dto.project.lifecycle.ProjectLifecycleDecisionDto;
import com.doc.dto.project.lifecycle.ProjectLifecycleResponseDto;
import com.doc.service.ProjectLifecycleRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/operationService/api/project-lifecycle-requests"
)
@RequiredArgsConstructor
public class ProjectLifecycleRequestController {

    private final ProjectLifecycleRequestService
            projectLifecycleRequestService;

    @PostMapping
    @Operation(
            summary = "CRT submits project force-close or reopen request"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Lifecycle request submitted successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid lifecycle request"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Only CRT user can submit request"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Project or user not found"
            )
    })
    public ResponseEntity<ProjectLifecycleResponseDto>
    createRequest(
            @Valid
            @RequestBody
            CreateProjectLifecycleRequestDto requestDto
    ) {
        ProjectLifecycleResponseDto response =
                projectLifecycleRequestService
                        .createRequest(requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{requestId}/decision")
    @Operation(
            summary = "ADMIN approves or rejects lifecycle request"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lifecycle request reviewed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request is invalid or already reviewed"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Only ADMIN can review request"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Lifecycle request, project, or user not found"
            )
    })
    public ResponseEntity<ProjectLifecycleResponseDto>
    reviewRequest(
            @PathVariable Long requestId,
            @Valid
            @RequestBody
            ProjectLifecycleDecisionDto decisionDto
    ) {
        ProjectLifecycleResponseDto response =
                projectLifecycleRequestService
                        .reviewRequest(
                                requestId,
                                decisionDto
                        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    @Operation(
            summary = "Get pending lifecycle requests for ADMIN"
    )
    public ResponseEntity<Page<ProjectLifecycleResponseDto>>
    getPendingRequests(
            @RequestParam Long adminUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ProjectLifecycleResponseDto> response =
                projectLifecycleRequestService
                        .getPendingRequests(
                                adminUserId,
                                page - 1,
                                size
                        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(
            summary = "Get lifecycle requests submitted by logged-in CRT user"
    )
    public ResponseEntity<Page<ProjectLifecycleResponseDto>>
    getMyRequests(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ProjectLifecycleResponseDto> response =
                projectLifecycleRequestService
                        .getMyRequests(
                                userId,
                                page - 1,
                                size
                        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    @Operation(
            summary = "Get complete lifecycle request history of project"
    )
    public ResponseEntity<List<ProjectLifecycleResponseDto>>
    getProjectRequestHistory(
            @PathVariable Long projectId,
            @RequestParam Long userId
    ) {
        List<ProjectLifecycleResponseDto> response =
                projectLifecycleRequestService
                        .getProjectRequestHistory(
                                projectId,
                                userId
                        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{requestId}")
    @Operation(
            summary = "Get lifecycle request details by ID"
    )
    public ResponseEntity<ProjectLifecycleResponseDto>
    getRequestById(
            @PathVariable Long requestId,
            @RequestParam Long userId
    ) {
        ProjectLifecycleResponseDto response =
                projectLifecycleRequestService
                        .getRequestById(
                                requestId,
                                userId
                        );

        return ResponseEntity.ok(response);
    }
}