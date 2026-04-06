package com.doc.impl;

import com.doc.dto.desigantion.DesignationRequestDto;
import com.doc.dto.desigantion.DesignationResponseDto;
import com.doc.entity.department.Department;
import com.doc.entity.department.Designation;
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
        Designation designation = designationRepository.findActiveUserById(id)
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

        Designation designation = designationRepository.findActiveUserById(id)
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
        Designation designation = designationRepository.findActiveUserById(id)
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
    public DesignationResponseDto createDesignationName(Long id, String name, Long weightValue) {
        logger.info("Creating designation with ID: {}, name: {}", id, name);

        validateCreateDesignation(id, name, weightValue);

        // Check for duplicate designation ID
        if (designationRepository.existsByIdAndIsDeletedFalse(id)) {
            logger.warn("Designation with ID {} already exists", id);
            throw new ValidationException("Designation with ID " + id + " already exists",
                    "ERR_DUPLICATE_DESIGNATION_ID");
        }

        Designation designation = new Designation();
        designation.setId(id);
        designation.setName(name.trim());
        designation.setWeightValue(weightValue != null ? weightValue : 0L);
        designation.setDeleted(false);
        designation.setCreatedDate(new Date());
        designation.setUpdatedDate(new Date());

        // CRITICAL FIX: Do NOT set department to null if column doesn't allow it yet
        // For now, we skip setting department (it will remain null in Java, but we handle DB constraint)
        // designation.setDepartment(null);   // Removed - let it be null by default

        designation = designationRepository.save(designation);

        logger.info("Designation created successfully with ID: {}", id);

        // Manually build response DTO (without using mapToResponseDto)
        DesignationResponseDto responseDto = new DesignationResponseDto();
        responseDto.setId(designation.getId());
        responseDto.setName(designation.getName());
        responseDto.setWeightValue(designation.getWeightValue());
        responseDto.setCreatedDate(designation.getCreatedDate());
        responseDto.setUpdatedDate(designation.getUpdatedDate());

        // Department info will be null since we didn't assign any
        responseDto.setDepartmentId(null);
        responseDto.setDepartmentName(null);

        return responseDto;
    }


    @Override
    public DesignationResponseDto mapDesignationToDepartment(List<Long> designationIds, Long departmentId) {
        if (designationIds == null || designationIds.isEmpty()) {
            throw new ValidationException("Designation ID list cannot be null or empty", "ERR_EMPTY_DESIGNATION_LIST");
        }

        logger.info("Mapping {} designations to department ID: {}", designationIds.size(), departmentId);

        // Validate department exists and is not deleted
        Department department = departmentRepository.findById(departmentId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> {
                    logger.error("Department with ID {} not found or is deleted", departmentId);
                    return new ResourceNotFoundException("Department with ID " + departmentId + " not found",
                            "ERR_DEPARTMENT_NOT_FOUND");
                });

        int successCount = 0;
        Long lastMappedId = null;

        for (Long designationId : designationIds) {
            if (designationId == null) {
                logger.warn("Skipping null designation ID in list");
                continue;
            }

            try {
                Designation designation = designationRepository.findActiveUserById(designationId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Designation with ID " + designationId + " not found or deleted",
                                "ERR_DESIGNATION_NOT_FOUND"));

                designation.setDepartment(department);
                designation.setUpdatedDate(new Date());

                designationRepository.save(designation);
                successCount++;
                lastMappedId = designationId;

                logger.debug("Successfully mapped designation ID {} to department {}", designationId, departmentId);

            } catch (ResourceNotFoundException e) {
                logger.warn("Designation ID {} not found, skipping...", designationId);
                // Continue with other IDs instead of failing entire operation
            }
        }

        if (successCount == 0) {
            throw new ResourceNotFoundException("No valid designations found to map", "ERR_NO_DESIGNATIONS_MAPPED");
        }

        logger.info("Successfully mapped {} out of {} designations to department ID {}",
                successCount, designationIds.size(), departmentId);

        // Return response for the last successfully mapped designation (common pattern)
        // If you want to return something else, you can change this logic
        if (lastMappedId != null) {
            Designation lastDesignation = designationRepository.findActiveUserById(lastMappedId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Last mapped designation not found", "ERR_DESIGNATION_NOT_FOUND"));

            return mapToResponseDto(lastDesignation);
        }

        // Fallback (should not reach here)
        throw new ResourceNotFoundException("Failed to map designations", "ERR_MAPPING_FAILED");
    }

    // Private validation method
    private void validateCreateDesignation(Long id, String name, Long weightValue) {
        if (id == null) {
            throw new ValidationException("Designation ID cannot be null", "ERR_NULL_DESIGNATION_ID");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Designation name cannot be empty", "ERR_NULL_DESIGNATION_NAME");
        }
        if (weightValue != null && weightValue < 0) {
            throw new ValidationException("Weight value must be non-negative", "ERR_INVALID_WEIGHT_VALUE");
        }
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

//        // Check for duplicate name in department
//        if (designationRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(requestDto.getName().trim(), requestDto.getDepartmentId())) {
//            logger.warn("Designation with name '{}' already exists in department ID {}", requestDto.getName(), requestDto.getDepartmentId());
//            throw new ValidationException("Designation with name '" + requestDto.getName() + "' already exists in the department", "ERR_DUPLICATE_DESIGNATION_NAME");
//        }

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