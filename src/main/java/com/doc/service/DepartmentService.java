package com.doc.service;



import com.doc.dto.department.DepartmentResponseDto;

import java.util.List;

public interface DepartmentService {
    DepartmentResponseDto createDepartment(String departmentName, Long createdBy);
    DepartmentResponseDto getDepartmentById(Long id);
    List<DepartmentResponseDto> getAllDepartments(int page, int size);
    DepartmentResponseDto updateDepartment(Long id, String departmentName);
    void deleteDepartment(Long id);

    DepartmentResponseDto createMasterDepartment(String name);
}