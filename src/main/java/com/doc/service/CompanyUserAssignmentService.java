package com.doc.service;


import com.doc.dto.companyUserAssignment.CompanyUserAssignmentRequestDto;
import com.doc.dto.companyUserAssignment.CompanyUserAssignmentResponseDto;

import java.util.List;

public interface CompanyUserAssignmentService {

    CompanyUserAssignmentResponseDto createAssignment(CompanyUserAssignmentRequestDto requestDto);

    CompanyUserAssignmentResponseDto getAssignmentById(Long id);

    List<CompanyUserAssignmentResponseDto> getAllAssignments();

    CompanyUserAssignmentResponseDto updateAssignment(Long id, CompanyUserAssignmentRequestDto requestDto);

    CompanyUserAssignmentResponseDto getAssignmentByCompanyAndDepartment(Long companyId, Long departmentId);
}