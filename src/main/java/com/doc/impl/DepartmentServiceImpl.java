package com.doc.impl;

import com.doc.dto.department.DepartmentResponseDto;
import com.doc.entity.user.Department;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repsoitory.DepartmentRepository;
import com.doc.repsoitory.UserRepository;
import com.doc.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public DepartmentResponseDto createDepartment(String departmentName, Long createdBy) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            throw new ValidationException("Department name cannot be empty");
        }

        if (departmentRepository.existsByNameAndIsDeletedFalse(departmentName)) {
            throw new ValidationException("Department with name " + departmentName + " already exists");
        }

        User createdByUser = userRepository.findActiveUserById(createdBy)
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + createdBy + " not found"));

        Department department = new Department();
        department.setName(departmentName.trim());
        department.setCreatedDate(new Date());
        department.setUpdatedDate(new Date());

        department = departmentRepository.save(department);
        return mapToResponseDto(department);
    }

    @Override
    public DepartmentResponseDto getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + id + " not found"));
        return mapToResponseDto(department);
    }

    @Override
    public List<DepartmentResponseDto> getAllDepartments(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Department> departmentPage = departmentRepository.findByIsDeletedFalse(pageable);
        return departmentPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentResponseDto updateDepartment(Long id, String departmentName) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            throw new ValidationException("Department name cannot be empty");
        }

        Department department = departmentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + id + " not found"));

        if (!department.getName().equals(departmentName.trim()) &&
                departmentRepository.existsByNameAndIsDeletedFalse(departmentName.trim())) {
            throw new ValidationException("Department with name " + departmentName + " already exists");
        }

        department.setName(departmentName.trim());
        department.setUpdatedDate(new Date());
        department = departmentRepository.save(department);
        return mapToResponseDto(department);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + id + " not found"));

        department.setDeleted(true);
        department.setUpdatedDate(new Date());
        departmentRepository.save(department);
    }

    private DepartmentResponseDto mapToResponseDto(Department department) {
        DepartmentResponseDto dto = new DepartmentResponseDto();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setCreatedDate(department.getCreatedDate());
        dto.setUpdatedDate(department.getUpdatedDate());
        return dto;
    }
}