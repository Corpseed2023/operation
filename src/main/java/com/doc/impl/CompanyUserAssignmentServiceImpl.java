package com.doc.impl;


import com.doc.dto.companyUserAssignment.CompanyUserAssignmentRequestDto;
import com.doc.dto.companyUserAssignment.CompanyUserAssignmentResponseDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.CompanyUserAssignment;
import com.doc.entity.user.Department;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.CompanyRepository;
import com.doc.repository.CompanyUserAssignmentRepository;
import com.doc.repository.DepartmentRepository;
import com.doc.repository.UserRepository;
import com.doc.service.CompanyUserAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompanyUserAssignmentServiceImpl implements CompanyUserAssignmentService {

    @Autowired
    private CompanyUserAssignmentRepository assignmentRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CompanyUserAssignmentResponseDto createAssignment(CompanyUserAssignmentRequestDto requestDto) {
        validateRequestDto(requestDto);

        if (assignmentRepository.existsByCompanyIdAndDepartmentIdAndIsDeletedFalseAndIsActiveTrue(requestDto.getCompanyId(), requestDto.getDepartmentId())) {
            throw new ValidationException("Assignment for company ID " + requestDto.getCompanyId() + " and department ID " + requestDto.getDepartmentId() + " already exists", "DUPLICATE_ASSIGNMENT");
        }

        Company company = companyRepository.findActiveUserById(requestDto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company with ID " + requestDto.getCompanyId() + " not found or is deleted", "COMPANY_NOT_FOUND"));

        Department department = departmentRepository.findByIdAndIsDeletedFalse(requestDto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + requestDto.getDepartmentId() + " not found or is deleted", "DEPARTMENT_NOT_FOUND"));

        User primaryUser = userRepository.findActiveUserById(requestDto.getPrimaryUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Primary user with ID " + requestDto.getPrimaryUserId() + " not found or is deleted", "USER_NOT_FOUND"));

        User alternativeUser = null;
        if (requestDto.getAlternativeUserId() != null) {
            alternativeUser = userRepository.findActiveUserById(requestDto.getAlternativeUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Alternative user with ID " + requestDto.getAlternativeUserId() + " not found or is deleted", "USER_NOT_FOUND"));
        }

        User createdBy = userRepository.findActiveUserById(requestDto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Created by user with ID " + requestDto.getCreatedBy() + " not found or is deleted", "USER_NOT_FOUND"));

        User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Updated by user with ID " + requestDto.getUpdatedBy() + " not found or is deleted", "USER_NOT_FOUND"));

        CompanyUserAssignment assignment = new CompanyUserAssignment();
        assignment.setCompany(company);
        assignment.setDepartment(department);
        assignment.setPrimaryUser(primaryUser);
        assignment.setAlternativeUser(alternativeUser);
        assignment.setCreatedBy(requestDto.getCreatedBy());
        assignment.setUpdatedBy(requestDto.getUpdatedBy());
        assignment.setCreatedDate(new Date());
        assignment.setUpdatedDate(new Date());
        assignment.setDeleted(false);
        assignment.setActive(true);

        assignment = assignmentRepository.save(assignment);
        return mapToResponseDto(assignment);
    }

    @Override
    public CompanyUserAssignmentResponseDto getAssignmentById(Long id) {
        CompanyUserAssignment assignment = assignmentRepository.findById(id)
                .filter(a -> !a.isDeleted() && a.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment with ID " + id + " not found or is deleted/inactive", "ASSIGNMENT_NOT_FOUND"));
        return mapToResponseDto(assignment);
    }

    @Override
    public List<CompanyUserAssignmentResponseDto> getAllAssignments() {
        return assignmentRepository.findAll().stream()
                .filter(a -> !a.isDeleted() && a.isActive())
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompanyUserAssignmentResponseDto updateAssignment(Long id, CompanyUserAssignmentRequestDto requestDto) {
        validateRequestDto(requestDto);

        CompanyUserAssignment assignment = assignmentRepository.findById(id)
                .filter(a -> !a.isDeleted() && a.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment with ID " + id + " not found or is deleted/inactive", "ASSIGNMENT_NOT_FOUND"));

        Company company = companyRepository.findActiveUserById(requestDto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company with ID " + requestDto.getCompanyId() + " not found or is deleted", "COMPANY_NOT_FOUND"));

        Department department = departmentRepository.findByIdAndIsDeletedFalse(requestDto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + requestDto.getDepartmentId() + " not found or is deleted", "DEPARTMENT_NOT_FOUND"));

        User primaryUser = userRepository.findActiveUserById(requestDto.getPrimaryUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Primary user with ID " + requestDto.getPrimaryUserId() + " not found or is deleted", "USER_NOT_FOUND"));

        User alternativeUser = null;
        if (requestDto.getAlternativeUserId() != null) {
            alternativeUser = userRepository.findActiveUserById(requestDto.getAlternativeUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Alternative user with ID " + requestDto.getAlternativeUserId() + " not found or is deleted", "USER_NOT_FOUND"));
        }

        User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Updated by user with ID " + requestDto.getUpdatedBy() + " not found or is deleted", "USER_NOT_FOUND"));

        assignment.setCompany(company);
        assignment.setDepartment(department);
        assignment.setPrimaryUser(primaryUser);
        assignment.setAlternativeUser(alternativeUser);
        assignment.setUpdatedBy(requestDto.getUpdatedBy());
        assignment.setUpdatedDate(new Date());

        assignment = assignmentRepository.save(assignment);
        return mapToResponseDto(assignment);
    }



    @Override
    public CompanyUserAssignmentResponseDto getAssignmentByCompanyAndDepartment(Long companyId, Long departmentId) {
        CompanyUserAssignment assignment = assignmentRepository.findByCompanyIdAndDepartmentId(companyId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment for company ID " + companyId + " and department ID " + departmentId + " not found", "ASSIGNMENT_NOT_FOUND"));
        return mapToResponseDto(assignment);
    }

    private void validateRequestDto(CompanyUserAssignmentRequestDto requestDto) {
        if (requestDto.getCompanyId() == null) {
            throw new ValidationException("Company ID cannot be null", "INVALID_COMPANY_ID");
        }
        if (requestDto.getDepartmentId() == null) {
            throw new ValidationException("Department ID cannot be null", "INVALID_DEPARTMENT_ID");
        }
        if (requestDto.getPrimaryUserId() == null) {
            throw new ValidationException("Primary user ID cannot be null", "INVALID_PRIMARY_USER_ID");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null", "INVALID_CREATED_BY");
        }
        if (requestDto.getUpdatedBy() == null) {
            throw new ValidationException("Updated by user ID cannot be null", "INVALID_UPDATED_BY");
        }
    }

    private CompanyUserAssignmentResponseDto mapToResponseDto(CompanyUserAssignment assignment) {
        CompanyUserAssignmentResponseDto dto = new CompanyUserAssignmentResponseDto();
        dto.setId(assignment.getId());
        dto.setCompanyId(assignment.getCompany().getId());
        dto.setDepartmentId(assignment.getDepartment().getId());
        dto.setPrimaryUserId(assignment.getPrimaryUser().getId());
        dto.setAlternativeUserId(assignment.getAlternativeUser() != null ? assignment.getAlternativeUser().getId() : null);
        dto.setDeleted(assignment.isDeleted());
        dto.setActive(assignment.isActive());
        dto.setCreatedDate(assignment.getCreatedDate());
        dto.setUpdatedDate(assignment.getUpdatedDate());
        dto.setCreatedBy(assignment.getCreatedBy());
        dto.setUpdatedBy(assignment.getUpdatedBy());
        return dto;
    }
}