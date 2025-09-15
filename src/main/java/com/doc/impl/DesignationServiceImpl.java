package com.doc.impl;

import com.doc.dto.desigantion.DesignationRequestDto;
import com.doc.dto.desigantion.DesignationResponseDto;
import com.doc.entity.user.Department;
import com.doc.entity.user.Designation;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.DepartmentRepository;
import com.doc.repository.DesignationRepository;
import com.doc.repository.UserRepository;
import com.doc.service.DesignationService;
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
public class DesignationServiceImpl implements DesignationService {

    @Autowired
    private DesignationRepository designationRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public DesignationResponseDto createDesignation(DesignationRequestDto requestDto) {
        validateRequestDto(requestDto);

        // Check for duplicate designation ID
        if (designationRepository.existsById(requestDto.getId())) {
            throw new ValidationException("Designation with ID " + requestDto.getId() + " already exists");
        }

        // Check for duplicate name in department
        if (designationRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(requestDto.getName().trim(), requestDto.getDepartmentId())) {
            throw new ValidationException("Designation with name " + requestDto.getName() + " already exists in the department");
        }

        // Validate department
        Department department = departmentRepository.findById(requestDto.getDepartmentId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + requestDto.getDepartmentId() + " not found"));

        // Validate createdBy user
        userRepository.findActiveUserById(requestDto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getCreatedBy() + " not found"));

        Designation designation = new Designation();
        designation.setId(requestDto.getId());
        designation.setName(requestDto.getName().trim());
        designation.setWeightValue(requestDto.getWeightValue());
        designation.setDepartment(department);
        designation.setCreatedDate(new Date());
        designation.setUpdatedDate(new Date());
        designation.setDeleted(false);

        designation = designationRepository.save(designation);
        return mapToResponseDto(designation);
    }

    @Override
    public DesignationResponseDto getDesignationById(Long id) {
        Designation designation = designationRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Designation with ID " + id + " not found"));
        return mapToResponseDto(designation);
    }

    @Override
    public List<DesignationResponseDto> getAllDesignations(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Designation> designationPage = designationRepository.findByDepartmentIsDeletedFalse(pageable);
        return designationPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public DesignationResponseDto updateDesignation(Long id, DesignationRequestDto requestDto) {
        validateRequestDto(requestDto);

        Designation designation = designationRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Designation with ID " + id + " not found"));

        Department department = departmentRepository.findById(requestDto.getDepartmentId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + requestDto.getDepartmentId() + " not found"));

        if (!designation.getName().equals(requestDto.getName().trim()) &&
                designationRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(requestDto.getName().trim(), requestDto.getDepartmentId())) {
            throw new ValidationException("Designation with name " + requestDto.getName() + " already exists in the department");
        }

        designation.setName(requestDto.getName().trim());
        designation.setWeightValue(requestDto.getWeightValue());
        designation.setDepartment(department);
        designation.setUpdatedDate(new Date());
        designation = designationRepository.save(designation);
        return mapToResponseDto(designation);
    }

    @Override
    public void deleteDesignation(Long id) {
        Designation designation = designationRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Designation with ID " + id + " not found"));

        designation.setDeleted(true);
        designation.setUpdatedDate(new Date());
        designationRepository.save(designation);
    }

    @Override
    public DesignationResponseDto createMasterDesignation(DesignationRequestDto requestDto) {
        validateRequestDto(requestDto);

        // Check for duplicate designation ID
        if (designationRepository.existsById(requestDto.getId())) {
            throw new ValidationException("Designation with ID " + requestDto.getId() + " already exists");
        }

        // Check for duplicate name in department
        if (designationRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(requestDto.getName().trim(), requestDto.getDepartmentId())) {
            throw new ValidationException("Designation with name " + requestDto.getName() + " already exists in the department");
        }

        // Validate department
        Department department = departmentRepository.findById(requestDto.getDepartmentId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + requestDto.getDepartmentId() + " not found"));

        Designation designation = new Designation();
        designation.setId(requestDto.getId());
        designation.setName(requestDto.getName().trim());
        designation.setWeightValue(requestDto.getWeightValue());
        designation.setDepartment(department);
        designation.setCreatedDate(new Date());
        designation.setUpdatedDate(new Date());
        designation.setDeleted(false);

        designation = designationRepository.save(designation);
        return mapToResponseDto(designation);
    }

    private void validateRequestDto(DesignationRequestDto requestDto) {
        if (requestDto.getId() == null) {
            throw new ValidationException("Designation ID cannot be null");
        }
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new ValidationException("Designation name cannot be empty");
        }
        if (requestDto.getWeightValue() == null || requestDto.getWeightValue() < 0) {
            throw new ValidationException("Weight value must be a non-negative number");
        }
        if (requestDto.getDepartmentId() == null) {
            throw new ValidationException("Department ID cannot be null");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null");
        }
    }

    private DesignationResponseDto mapToResponseDto(Designation designation) {
        DesignationResponseDto dto = new DesignationResponseDto();
        dto.setId(designation.getId());
        dto.setName(designation.getName());
        dto.setWeightValue(designation.getWeightValue());
        dto.setDepartmentId(designation.getDepartment().getId());
        dto.setDepartmentName(designation.getDepartment().getName());
        dto.setCreatedDate(designation.getCreatedDate());
        dto.setUpdatedDate(designation.getUpdatedDate());
        return dto;
    }
}