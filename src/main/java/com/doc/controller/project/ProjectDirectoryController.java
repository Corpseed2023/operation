package com.doc.controller.project;

import com.doc.dto.project.directory.DirectoryDocumentRequestDto;
import com.doc.dto.project.directory.ProjectDirectoryResponseDto;
import com.doc.service.project.ProjectDirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/projects/{projectId}/directories")
public class ProjectDirectoryController {

    private final ProjectDirectoryService projectDirectoryService;

    public ProjectDirectoryController(
            ProjectDirectoryService projectDirectoryService
    ) {
        this.projectDirectoryService = projectDirectoryService;
    }

    @PostMapping
    @Operation(summary = "Create directory for a project")
    public ResponseEntity<ProjectDirectoryResponseDto> createDirectory(
            @PathVariable Long projectId,
            @RequestParam String directoryName,
            @RequestParam Long userId
    ) {
        ProjectDirectoryResponseDto response =
                projectDirectoryService.createDirectory(
                        projectId,
                        directoryName,
                        userId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{directoryId}/documents")
    @Operation(summary = "Add document into project directory")
    public ResponseEntity<ProjectDirectoryResponseDto> uploadDocuments(
            @PathVariable Long projectId,
            @PathVariable Long directoryId,
            @RequestParam Long userId,
            @Valid @RequestBody
            DirectoryDocumentRequestDto directoryDocumentRequestDto
    ) {
        ProjectDirectoryResponseDto response =
                projectDirectoryService.uploadDocuments(
                        projectId,
                        directoryId,
                        userId,
                        directoryDocumentRequestDto
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @GetMapping
    @Operation(summary = "Get all project directories with documents")
    public ResponseEntity<List<ProjectDirectoryResponseDto>>
    getDirectories(@PathVariable Long projectId) {

        return ResponseEntity.ok(
                projectDirectoryService.getProjectDirectories(projectId)
        );
    }
}