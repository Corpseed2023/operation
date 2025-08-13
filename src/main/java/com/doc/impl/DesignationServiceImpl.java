package com.doc.impl;


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
    public DesignationResponseDto createDesignation(String name, Long weightValue, Long departmentId, Long createdBy) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Designation name cannot be empty");
        }
        if (weightValue == null || weightValue < 0) {
            throw new ValidationException("Weight value must be a non-negative number");
        }
        if (departmentId == null) {
            throw new ValidationException("Department ID cannot be null");
        }

        if (designationRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(name.trim(), departmentId)) {
            throw new ValidationException("Designation with name " + name + " already exists in the department");
        }

        Department department = departmentRepository.findById(departmentId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + departmentId + " not found"));

        User createdByUser = userRepository.findActiveUserById(createdBy)
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + createdBy + " not found"));

        Designation designation = new Designation();
        designation.setName(name.trim());
        designation.setWeightValue(weightValue);
        designation.setDepartment(department);
        designation.setCreatedDate(new Date());
        designation.setUpdatedDate(new Date());

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
    public DesignationResponseDto updateDesignation(Long id, String name, Long weightValue, Long departmentId) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Designation name cannot be empty");
        }

        if (weightValue == null || weightValue < 0) {
            throw new ValidationException("Weight value must be a non-negative number");
        }

        if (departmentId == null) {
            throw new ValidationException("Department ID cannot be null");
        }

        Designation designation = designationRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Designation with ID " + id + " not found"));

        Department department = departmentRepository.findById(departmentId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + departmentId + " not found"));

        if (!designation.getName().equals(name.trim()) &&
                designationRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(name.trim(), departmentId)) {
            throw new ValidationException("Designation with name " + name + " already exists in the department");
        }

        designation.setName(name.trim());
        designation.setWeightValue(weightValue);
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


    @Override
    public DesignationResponseDto createMasterDesignation(String name, Long weightValue, Long departmentId) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Designation name cannot be empty");
        }
        if (weightValue == null || weightValue < 0) {
            throw new ValidationException("Weight value must be a non-negative number");
        }
        if (departmentId == null) {
            throw new ValidationException("Department ID cannot be null");
        }

        if (designationRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(name.trim(), departmentId)) {
            throw new ValidationException("Designation with name " + name + " already exists in the department");
        }

        Department department = departmentRepository.findById(departmentId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + departmentId + " not found"));

        Designation designation = new Designation();
        designation.setName(name.trim());
        designation.setWeightValue(weightValue);
        designation.setDepartment(department);
        designation.setCreatedDate(new Date());
        designation.setUpdatedDate(new Date());

        designation = designationRepository.save(designation);
        return mapToResponseDto(designation);
    }



}
