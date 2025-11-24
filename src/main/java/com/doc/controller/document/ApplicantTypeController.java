// src/main/java/com/doc/controller/document/ApplicantTypeController.java
package com.doc.controller.document;

import com.doc.dto.document.ApplicantTypeRequestDto;
import com.doc.dto.document.ApplicantTypeResponseDto;
import com.doc.service.ApplicantTypeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    @PostMapping
    public ResponseEntity<ApplicantTypeResponseDto> createApplicantType(
            @Valid @RequestBody ApplicantTypeRequestDto requestDto) {
        ApplicantTypeResponseDto response = applicantTypeService.createApplicantType(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing applicant type")
    @PutMapping("/{id}")
    public ResponseEntity<ApplicantTypeResponseDto> updateApplicantType(
            @PathVariable Long id,
            @Valid @RequestBody ApplicantTypeRequestDto requestDto) {
        ApplicantTypeResponseDto response = applicantTypeService.updateApplicantType(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Soft delete an applicant type")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplicantType(@PathVariable Long id) {
        applicantTypeService.deleteApplicantType(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get applicant type by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicantTypeResponseDto> getApplicantTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(applicantTypeService.getApplicantTypeById(id));
    }

    @Operation(summary = "Get all active applicant types (dropdown friendly)")
    @GetMapping("/active")
    public ResponseEntity<List<ApplicantTypeResponseDto>> getAllActiveApplicantTypes() {
        return ResponseEntity.ok(applicantTypeService.getAllActiveApplicantTypes());
    }

    @Operation(summary = "Get applicant types with pagination (page starts from 1)")
    @GetMapping
    public ResponseEntity<List<ApplicantTypeResponseDto>> getApplicantTypesPaginated(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<ApplicantTypeResponseDto> result = applicantTypeService.getApplicantTypesPaginated(page, size);
        return ResponseEntity.ok(result);
    }
}