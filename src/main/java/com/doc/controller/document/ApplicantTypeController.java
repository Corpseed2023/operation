package com.doc.controller.document;

import com.doc.dto.document.ApplicantTypeRequestDto;
import com.doc.dto.document.ApplicantTypeResponseDto;
import com.doc.service.ApplicantTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applicant-types")
@RequiredArgsConstructor
@Tag(name = "Applicant Types", description = "Manage applicant type master data")
public class ApplicantTypeController {

    private final ApplicantTypeService applicantTypeService;

    @Operation(summary = "Create a new applicant type")
    @PostMapping
    public ResponseEntity<ApplicantTypeResponseDto> createApplicantType(
            @Valid @RequestBody ApplicantTypeRequestDto requestDto) {

        ApplicantTypeResponseDto response = applicantTypeService.createApplicantType(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update an existing applicant type")
    @PutMapping("/{id}")
    public ResponseEntity<ApplicantTypeResponseDto> updateApplicantType(
            @PathVariable Long id,
            @Valid @RequestBody ApplicantTypeRequestDto requestDto) {

        ApplicantTypeResponseDto response = applicantTypeService.updateApplicantType(id, requestDto);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get applicant type by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicantTypeResponseDto> getApplicantTypeById(@PathVariable Long id) {
        ApplicantTypeResponseDto response = applicantTypeService.getApplicantTypeById(id);
        return ResponseEntity.ok(response);
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