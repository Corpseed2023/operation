package com.doc.controller.project;

import com.doc.service.ProjectStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import com.doc.dto.project.status.ProjectStatusRequestDto;
import com.doc.dto.project.status.ProjectStatusResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/project-statuses")
public class ProjectStatusController {

    @Autowired
    private ProjectStatusService statusService;

    @Operation(summary = "Create a new project status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Status created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "409", description = "Status name already exists")
    })
    @PostMapping
    public ResponseEntity<ProjectStatusResponseDto> createStatus(@Valid @RequestBody ProjectStatusRequestDto requestDto) {
        ProjectStatusResponseDto response = statusService.createStatus(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing project status")
    @PutMapping("/{id}")
    public ResponseEntity<ProjectStatusResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProjectStatusRequestDto requestDto) {
        ProjectStatusResponseDto response = statusService.updateStatus(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all project statuses")
    @GetMapping
    public ResponseEntity<List<ProjectStatusResponseDto>> getAllStatuses() {
        List<ProjectStatusResponseDto> statuses = statusService.getAllStatuses();
        return ResponseEntity.ok(statuses);
    }

    @Operation(summary = "Get project status by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectStatusResponseDto> getStatusById(@PathVariable Long id) {
        ProjectStatusResponseDto response = statusService.getStatusById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete (soft) a project status")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatus(@PathVariable Long id) {
        statusService.deleteStatus(id);
        return ResponseEntity.noContent().build();
    }
}