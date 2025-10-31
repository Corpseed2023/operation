package com.doc.impl;

import com.doc.dto.role.RoleRequestDto;
import com.doc.dto.role.RoleResponseDto;
import com.doc.entity.user.Role;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.RoleRepository;
import com.doc.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public RoleResponseDto createRole(RoleRequestDto requestDto) {
        logger.info("Creating role with ID: {}, name: {}", requestDto.getId(), requestDto.getName());
        validateRequestDto(requestDto);

        if (roleRepository.existsByIdAndIsDeletedFalse(requestDto.getId())) {
            logger.warn("Role with ID {} already exists", requestDto.getId());
            throw new ValidationException("Role with ID " + requestDto.getId() + " already exists", "ERR_DUPLICATE_ID");
        }

        if (roleRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
            logger.warn("Role with name '{}' already exists", requestDto.getName());
            throw new ValidationException("Role with name '" + requestDto.getName() + "' already exists", "ERR_DUPLICATE_NAME");
        }

        Role role = new Role();
        role.setId(requestDto.getId());
        role.setName(requestDto.getName().trim());
        role.setCreatedBy(requestDto.getCreatedBy());
        role.setUpdatedBy(requestDto.getUpdatedBy());
        role.setDeleted(false);
        role = roleRepository.save(role);
        logger.info("Role created successfully with ID: {}", role.getId());

        return mapToResponseDto(role);
    }

    @Override
    public RoleResponseDto getRoleById(Long id) {
        logger.info("Fetching role with ID: {}", id);
        Role role = roleRepository.findActiveUserById(id)
                .orElseThrow(() -> {
                    logger.error("Role not found with ID: {}", id);
                    return new ResourceNotFoundException("Role with ID " + id + " not found", "ERR_ROLE_NOT_FOUND");
                });
        return mapToResponseDto(role);
    }

    @Override
    public Page<RoleResponseDto> getAllRoles(int page, int size, String name) {
        logger.info("Fetching roles, page: {}, size: {}, name: {}", page, size, name);
        if (page < 0 || size <= 0) {
            logger.warn("Invalid pagination parameters: page={}, size={}", page, size);
            throw new ValidationException("Page must be non-negative and size must be positive", "ERR_INVALID_PAGINATION");
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<Role> rolePage = name != null && !name.isBlank()
                ? roleRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(name.trim(), pageable)
                : roleRepository.findByIsDeletedFalse(pageable);

        if (rolePage.isEmpty() && name != null) {
            logger.warn("No roles found with name containing: {}", name);
            throw new ResourceNotFoundException("No roles found with name containing: " + name, "ERR_ROLES_NOT_FOUND");
        }

        return rolePage.map(this::mapToResponseDto);
    }

    @Override
    public RoleResponseDto updateRole(Long id, RoleRequestDto requestDto) {
        logger.info("Updating role with ID: {}", id);
        validateRequestDto(requestDto);

        Role role = roleRepository.findActiveUserById(id)
                .orElseThrow(() -> {
                    logger.error("Role not found with ID: {}", id);
                    return new ResourceNotFoundException("Role with ID " + id + " not found", "ERR_ROLE_NOT_FOUND");
                });

        if (!role.getName().equals(requestDto.getName().trim()) &&
                roleRepository.existsByNameAndIsDeletedFalseAndIdNot(requestDto.getName().trim(), id)) {
            logger.warn("Role with name '{}' already exists", requestDto.getName());
            throw new ValidationException("Role with name '" + requestDto.getName() + "' already exists", "ERR_DUPLICATE_NAME");
        }

        role.setName(requestDto.getName().trim());
        role.setUpdatedBy(requestDto.getUpdatedBy());
        role.setUpdatedDate(new Date());
        role = roleRepository.save(role);
        logger.info("Role updated successfully with ID: {}", id);

        return mapToResponseDto(role);
    }

    @Override
    public void deleteRole(Long id) {
        logger.info("Deleting role with ID: {}", id);
        Role role = roleRepository.findActiveUserById(id)
                .orElseThrow(() -> {
                    logger.error("Role not found with ID: {}", id);
                    return new ResourceNotFoundException("Role with ID " + id + " not found", "ERR_ROLE_NOT_FOUND");
                });

        role.setDeleted(true);
        role.setUpdatedBy(id);  // Assume updatedBy is passed or set to admin ID
        role.setUpdatedDate(new Date());
        roleRepository.save(role);
        logger.info("Role soft-deleted successfully with ID: {}", id);
    }

    private void validateRequestDto(RoleRequestDto requestDto) {
        if (requestDto.getId() == null) {
            logger.warn("Role ID cannot be null");
            throw new ValidationException("Role ID cannot be null", "ERR_NULL_ID");
        }
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            logger.warn("Role name is mandatory");
            throw new ValidationException("Role name is mandatory", "ERR_NULL_NAME");
        }
        if (requestDto.getCreatedBy() == null) {
            logger.warn("Created by user ID cannot be null");
            throw new ValidationException("Created by user ID cannot be null", "ERR_NULL_CREATED_BY");
        }
        if (requestDto.getUpdatedBy() == null) {
            logger.warn("Updated by user ID cannot be null");
            throw new ValidationException("Updated by user ID cannot be null", "ERR_NULL_UPDATED_BY");
        }
    }

    private RoleResponseDto mapToResponseDto(Role role) {
        RoleResponseDto dto = new RoleResponseDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setCreatedBy(role.getCreatedBy());
        dto.setUpdatedBy(role.getUpdatedBy());
        dto.setCreatedDate(role.getCreatedDate());
        dto.setUpdatedDate(role.getUpdatedDate());
        dto.setDeleted(role.isDeleted());
        return dto;
    }
}