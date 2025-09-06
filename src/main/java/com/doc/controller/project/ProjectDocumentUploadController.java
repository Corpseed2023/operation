package com.doc.controller.project;

import com.doc.dto.project.DocumentResponseDto;
import com.doc.dto.project.ProjectDocumentUploadRequestDto;
import com.doc.dto.project.ProjectDocumentStatusUpdateDto;
import com.doc.service.ProjectDocumentUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for managing project document uploads and status updates.
 */
@RestController
@RequestMapping("/api/projects")
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class ProjectDocumentUploadController {

    @Autowired
    private ProjectDocumentUploadService projectDocumentUploadService;

    @Operation(summary = "Upload a document for a specific milestone assignment in a project",
            description = "Uploads a document associated with a project milestone. Requires UPLOADER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project, milestone assignment, or required document not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate document upload")
    })
//    @PreAuthorize("hasRole('UPLOADER')")
    @PostMapping("/{projectId}/milestones/{milestoneAssignmentId}/documents")
    public ResponseEntity<DocumentResponseDto> uploadDocument(
            @Parameter(description = "ID of the project") @PathVariable Long projectId,
            @Parameter(description = "ID of the milestone assignment") @PathVariable Long milestoneAssignmentId,
            @Valid @RequestBody ProjectDocumentUploadRequestDto requestDto) {
        requestDto.setProjectId(projectId);
        requestDto.setMilestoneAssignmentId(milestoneAssignmentId);
        DocumentResponseDto response = projectDocumentUploadService.uploadDocument(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update the status of an uploaded document",
            description = "Updates the status of a document (e.g., to VERIFIED or REJECTED). Requires ADMIN or VERIFIER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or transition"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
//    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIER')")
    @PutMapping("/documents/{documentId}/status")
    public ResponseEntity<DocumentResponseDto> updateDocumentStatus(
            @Parameter(description = "UUID of the document to update") @PathVariable UUID documentId,
            @Valid @RequestBody ProjectDocumentStatusUpdateDto updateDto) {
        DocumentResponseDto response = projectDocumentUploadService.updateDocumentStatus(documentId, updateDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}