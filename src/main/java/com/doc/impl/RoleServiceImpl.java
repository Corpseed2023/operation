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

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public RoleResponseDto createRole(RoleRequestDto requestDto) {
        logger.info("Creating role with name: {}", requestDto.getName());
        if (roleRepository.existsByNameAndIdNot(requestDto.getName(), 0L)) {
            throw new ValidationException("Role with name '" + requestDto.getName() + "' already exists");
        }

        Role role = new Role();
        role.setName(requestDto.getName().trim());
        role = roleRepository.save(role);
        logger.info("Role created with ID: {}", role.getId());

        return mapToResponseDto(role);
    }

    @Override
    public RoleResponseDto getRoleById(Long id) {
        logger.info("Fetching role with ID: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + id));
        return mapToResponseDto(role);
    }

    @Override
    public Page<RoleResponseDto> getAllRoles(int page, int size, String name) {
        logger.info("Fetching roles, page: {}, size: {}, name: {}", page, size, name);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Role> rolePage = name != null && !name.isBlank()
                ? roleRepository.findByNameContaining(name.trim(), pageable)
                : roleRepository.findAll(pageable);

        return rolePage.map(this::mapToResponseDto);
    }

    @Override
    public RoleResponseDto updateRole(Long id, RoleRequestDto requestDto) {
        logger.info("Updating role with ID: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + id));

        if (roleRepository.existsByNameAndIdNot(requestDto.getName(), id)) {
            throw new ValidationException("Role with name '" + requestDto.getName() + "' already exists");
        }

        role.setName(requestDto.getName().trim());
        role = roleRepository.save(role);
        logger.info("Role updated with ID: {}", id);

        return mapToResponseDto(role);
    }

    @Override
    public void deleteRole(Long id) {
        logger.info("Deleting role with ID: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + id));
        roleRepository.delete(role);
        logger.info("Role deleted with ID: {}", id);
    }

    private RoleResponseDto mapToResponseDto(Role role) {
        RoleResponseDto dto = new RoleResponseDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        return dto;
    }
}
