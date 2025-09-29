package com.doc.controller.client;


import com.doc.dto.companyUserAssignment.CompanyUserAssignmentRequestDto;
import com.doc.dto.companyUserAssignment.CompanyUserAssignmentResponseDto;
import com.doc.service.CompanyUserAssignmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company-user-assignments")
public class CompanyUserAssignmentController {

    @Autowired
    private CompanyUserAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<CompanyUserAssignmentResponseDto> createAssignment(@Valid @RequestBody CompanyUserAssignmentRequestDto requestDto) {
        CompanyUserAssignmentResponseDto response = assignmentService.createAssignment(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyUserAssignmentResponseDto> getAssignmentById(@PathVariable Long id) {
        CompanyUserAssignmentResponseDto response = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CompanyUserAssignmentResponseDto>> getAllAssignments() {
        List<CompanyUserAssignmentResponseDto> responses = assignmentService.getAllAssignments();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyUserAssignmentResponseDto> updateAssignment(@PathVariable Long id, @Valid @RequestBody CompanyUserAssignmentRequestDto requestDto) {
        CompanyUserAssignmentResponseDto response = assignmentService.updateAssignment(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/company/{companyId}/department/{departmentId}")
    public ResponseEntity<CompanyUserAssignmentResponseDto> getAssignmentByCompanyAndDepartment(
            @PathVariable Long companyId,
            @PathVariable Long departmentId) {
        CompanyUserAssignmentResponseDto response = assignmentService.getAssignmentByCompanyAndDepartment(companyId, departmentId);
        return ResponseEntity.ok(response);
    }
}