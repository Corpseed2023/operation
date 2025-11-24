// src/main/java/com/doc/controller/document/ApplicantTypeController.java
package com.doc.controller.document;

import com.doc.dto.document.ApplicantTypeRequestDto;
import com.doc.dto.document.ApplicantTypeResponseDto;
import com.doc.service.ApplicantTypeService;
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
@RequestMapping("/api/applicant-types")
public class ApplicantTypeController {

    @Autowired
    private ApplicantTypeService applicantTypeService;

    @Operation(summary = "Create a new applicant type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Applicant type created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate name"),
            @ApiResponse(responseCode = "409", description = "Applicant type name already exists")
    })
    @PostMapping
    public ResponseEntity<ApplicantTypeResponseDto> createApplicantType(
            @Valid @RequestBody ApplicantTypeRequestDto requestDto) {
        ApplicantTypeResponseDto response = applicantTypeService.createApplicantType(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing applicant type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applicant type updated successfully"),
            @ApiResponse(responseCode = "404", description = "Applicant type not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate name conflict")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApplicantTypeResponseDto> updateApplicantType(
            @PathVariable Long id,
            @Valid @RequestBody ApplicantTypeRequestDto requestDto) {
        ApplicantTypeResponseDto response = applicantTypeService.updateApplicantType(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Soft delete an applicant type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Applicant type deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Applicant type not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplicantType(@PathVariable Long id) {
        applicantTypeService.deleteApplicantType(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get applicant type by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applicant type found"),
            @ApiResponse(responseCode = "404", description = "Applicant type not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApplicantTypeResponseDto> getApplicantTypeById(@PathVariable Long id) {
        ApplicantTypeResponseDto response = applicantTypeService.getApplicantTypeById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all active applicant types (no pagination)")
    @ApiResponse(responseCode = "200", description = "List of active applicant types")
    @GetMapping("/active")
    public ResponseEntity<List<ApplicantTypeResponseDto>> getAllActiveApplicantTypes() {
        List<ApplicantTypeResponseDto> list = applicantTypeService.getAllActiveApplicantTypes();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get all applicant types with pagination")
    @GetMapping
    public ResponseEntity<Page<ApplicantTypeResponseDto>> getApplicantTypesPaged(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Page<ApplicantTypeResponseDto> result = applicantTypeService.getApplicantTypesPaged(page, size);
        return ResponseEntity.ok(result);
    }
}