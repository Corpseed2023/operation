package com.doc.service;

import com.doc.dto.department.DepartmentRequestDto;
import com.doc.dto.department.DepartmentResponseDto;

import java.util.List;

public interface DepartmentService {

    DepartmentResponseDto createDepartment(DepartmentRequestDto requestDto);

    DepartmentResponseDto getDepartmentById(Long id);

    List<DepartmentResponseDto> getAllDepartments(int page, int size);

    DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto requestDto);

    void deleteDepartment(Long id);

    DepartmentResponseDto createMasterDepartment(DepartmentRequestDto requestDto);
}