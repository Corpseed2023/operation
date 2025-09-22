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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing designation-related operations.
 */
@Service
@Transactional
public class DesignationServiceImpl implements DesignationService {

    private static final Logger logger = LoggerFactory.getLogger(DesignationServiceImpl.class);

    @Autowired
    private DesignationRepository designationRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public DesignationResponseDto createDesignation(DesignationRequestDto requestDto) {
        logger.info("Creating designation with ID: {}, name: {}, department ID: {}",
                requestDto.getId(), requestDto.getName(), requestDto.getDepartmentId());
        validateRequestDto(requestDto);

        // Check for duplicate designation ID
        if (designationRepository.existsByIdAndIsDeletedFalse(requestDto.getId())) {
            logger.warn("Designation with ID {} already exists", requestDto.getId());
            throw new ValidationException("Designation with ID " + requestDto.getId() + " already exists", "ERR_DUPLICATE_DESIGNATION_ID");
        }

        // Check for duplicate name in department
        if (designationRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(requestDto.getName().trim(), requestDto.getDepartmentId())) {
            logger.warn("Designation with name '{}' already exists in department ID {}", requestDto.getName(), requestDto.getDepartmentId());
            throw new ValidationException("Designation with name '" + requestDto.getName() + "' already exists in the department", "ERR_DUPLICATE_DESIGNATION_NAME");
        }

        // Validate department
        Department department = departmentRepository.findById(requestDto.getDepartmentId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> {
                    logger.error("Department with ID {} not found or is deleted", requestDto.getDepartmentId());
                    return new ResourceNotFoundException("Department with ID " + requestDto.getDepartmentId() + " not found", "ERR_DEPARTMENT_NOT_FOUND");
                });


        Designation designation = new Designation();
        designation.setId(requestDto.getId());
        designation.setName(requestDto.getName().trim());
        designation.setWeightValue(requestDto.getWeightValue());
        designation.setDepartment(department);
        designation.setCreatedDate(new Date());
        designation.setUpdatedDate(new Date());
        designation.setDeleted(false);

        designation = designationRepository.save(designation);
        logger.info("Designation created successfully with ID: {}", designation.getId());

        return mapToResponseDto(designation);
    }

    @Override
    public DesignationResponseDto getDesignationById(Long id) {
        logger.info("Fetching designation with ID: {}", id);
        Designation designation = designationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.error("Designation with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Designation with ID " + id + " not found", "ERR_DESIGNATION_NOT_FOUND");
                });
        return mapToResponseDto(designation);
    }

    @Override
    public List<DesignationResponseDto> getAllDesignations(int page, int size) {
        logger.info("Fetching all designations, page: {}, size: {}", page, size);
        if (page < 0 || size <= 0) {
            logger.warn("Invalid pagination parameters: page={}, size={}", page, size);
            throw new ValidationException("Page must be non-negative and size must be positive", "ERR_INVALID_PAGINATION");
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<Designation> designationPage = designationRepository.findByIsDeletedFalse(pageable);
        if (designationPage.isEmpty()) {
            logger.warn("No designations found for page: {}, size: {}", page, size);
            throw new ResourceNotFoundException("No designations found", "ERR_DESIGNATIONS_NOT_FOUND");
        }

        return designationPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public DesignationResponseDto updateDesignation(Long id, DesignationRequestDto requestDto) {
        logger.info("Updating designation with ID: {}", id);
        validateRequestDto(requestDto);

        Designation designation = designationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.error("Designation with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Designation with ID " + id + " not found", "ERR_DESIGNATION_NOT_FOUND");
                });

        Department department = departmentRepository.findById(requestDto.getDepartmentId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> {
                    logger.error("Department with ID {} not found or is deleted", requestDto.getDepartmentId());
                    return new ResourceNotFoundException("Department with ID " + requestDto.getDepartmentId() + " not found", "ERR_DEPARTMENT_NOT_FOUND");
                });

        if (!designation.getName().equals(requestDto.getName().trim()) &&
                designationRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(requestDto.getName().trim(), requestDto.getDepartmentId())) {
            logger.warn("Designation with name '{}' already exists in department ID {}", requestDto.getName(), requestDto.getDepartmentId());
            throw new ValidationException("Designation with name '" + requestDto.getName() + "' already exists in the department", "ERR_DUPLICATE_DESIGNATION_NAME");
        }

        designation.setName(requestDto.getName().trim());
        designation.setWeightValue(requestDto.getWeightValue());
        designation.setDepartment(department);
        designation.setUpdatedDate(new Date());
        designation = designationRepository.save(designation);
        logger.info("Designation updated successfully with ID: {}", id);

        return mapToResponseDto(designation);
    }

    @Override
    public void deleteDesignation(Long id) {
        logger.info("Deleting designation with ID: {}", id);
        Designation designation = designationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.error("Designation with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Designation with ID " + id + " not found", "ERR_DESIGNATION_NOT_FOUND");
                });

        designation.setDeleted(true);
        designation.setUpdatedDate(new Date());
        designationRepository.save(designation);
        logger.info("Designation soft-deleted successfully with ID: {}", id);
    }

    @Override
    public DesignationResponseDto createMasterDesignation(DesignationRequestDto requestDto) {
        logger.info("Creating master designation with ID: {}, name: {}, department ID: {}",
                requestDto.getId(), requestDto.getName(), requestDto.getDepartmentId());
        validateRequestDto(requestDto);

        // Check for duplicate designation ID
        if (designationRepository.existsByIdAndIsDeletedFalse(requestDto.getId())) {
            logger.warn("Designation with ID {} already exists", requestDto.getId());
            throw new ValidationException("Designation with ID " + requestDto.getId() + " already exists", "ERR_DUPLICATE_DESIGNATION_ID");
        }

        // Check for duplicate name in department
        if (designationRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(requestDto.getName().trim(), requestDto.getDepartmentId())) {
            logger.warn("Designation with name '{}' already exists in department ID {}", requestDto.getName(), requestDto.getDepartmentId());
            throw new ValidationException("Designation with name '" + requestDto.getName() + "' already exists in the department", "ERR_DUPLICATE_DESIGNATION_NAME");
        }

        // Validate department
        Department department = departmentRepository.findById(requestDto.getDepartmentId())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> {
                    logger.error("Department with ID {} not found or is deleted", requestDto.getDepartmentId());
                    return new ResourceNotFoundException("Department with ID " + requestDto.getDepartmentId() + " not found", "ERR_DEPARTMENT_NOT_FOUND");
                });

        Designation designation = new Designation();
        designation.setId(requestDto.getId());
        designation.setName(requestDto.getName().trim());
        designation.setWeightValue(requestDto.getWeightValue());
        designation.setDepartment(department);
        designation.setCreatedDate(new Date());
        designation.setUpdatedDate(new Date());
        designation.setDeleted(false);

        designation = designationRepository.save(designation);
        logger.info("Master designation created successfully with ID: {}", designation.getId());

        return mapToResponseDto(designation);
    }

    private void validateRequestDto(DesignationRequestDto requestDto) {
        if (requestDto.getId() == null) {
            logger.warn("Designation ID cannot be null");
            throw new ValidationException("Designation ID cannot be null", "ERR_NULL_DESIGNATION_ID");
        }
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            logger.warn("Designation name cannot be empty");
            throw new ValidationException("Designation name cannot be empty", "ERR_NULL_DESIGNATION_NAME");
        }
        if (requestDto.getWeightValue() == null || requestDto.getWeightValue() < 0) {
            logger.warn("Weight value must be a non-negative number, got: {}", requestDto.getWeightValue());
            throw new ValidationException("Weight value must be a non-negative number", "ERR_INVALID_WEIGHT_VALUE");
        }
        if (requestDto.getDepartmentId() == null) {
            logger.warn("Department ID cannot be null");
            throw new ValidationException("Department ID cannot be null", "ERR_NULL_DEPARTMENT_ID");
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