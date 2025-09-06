package com.doc.impl;

import com.doc.dto.department.DepartmentRequestDto;
import com.doc.dto.department.DepartmentResponseDto;
import com.doc.entity.user.Department;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.DepartmentRepository;
import com.doc.repository.UserRepository;
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
    public DepartmentResponseDto createDepartment(DepartmentRequestDto requestDto) {
        validateRequestDto(requestDto);

        // Check for duplicate department ID
        if (departmentRepository.existsById(requestDto.getId())) {
            throw new ValidationException("Department with ID " + requestDto.getId() + " already exists");
        }

        // Check for duplicate name
        if (departmentRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
            throw new ValidationException("Department with name " + requestDto.getName() + " already exists");
        }

        // Validate createdBy user
        userRepository.findActiveUserById(requestDto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getCreatedBy() + " not found"));

        Department department = new Department();
        department.setId(requestDto.getId());
        department.setName(requestDto.getName().trim());
        department.setCreatedDate(new Date());
        department.setUpdatedDate(new Date());
        department.setDeleted(false);

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
    public DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto requestDto) {
        validateRequestDto(requestDto);

        Department department = departmentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + id + " not found"));

        if (!department.getName().equals(requestDto.getName().trim()) &&
                departmentRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
            throw new ValidationException("Department with name " + requestDto.getName() + " already exists");
        }

        department.setName(requestDto.getName().trim());
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

    @Override
    public DepartmentResponseDto createMasterDepartment(DepartmentRequestDto requestDto) {
        validateRequestDto(requestDto);

        // Check for duplicate department ID
        if (departmentRepository.existsById(requestDto.getId())) {
            throw new ValidationException("Department with ID " + requestDto.getId() + " already exists");
        }

        // Check for duplicate name
        if (departmentRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
            throw new ValidationException("Department with name " + requestDto.getName() + " already exists");
        }

        Department department = new Department();
        department.setId(requestDto.getId());
        department.setName(requestDto.getName().trim());
        department.setCreatedDate(new Date());
        department.setUpdatedDate(new Date());
        department.setDeleted(false);

        department = departmentRepository.save(department);
        return mapToResponseDto(department);
    }

    private void validateRequestDto(DepartmentRequestDto requestDto) {
        if (requestDto.getId() == null) {
            throw new ValidationException("Department ID cannot be null");
        }
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new ValidationException("Department name cannot be empty");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null");
        }
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