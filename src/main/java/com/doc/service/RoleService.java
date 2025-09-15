package com.doc.service;

import com.doc.dto.role.RoleRequestDto;
import com.doc.dto.role.RoleResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RoleService {

    RoleResponseDto createRole(RoleRequestDto requestDto);

    RoleResponseDto getRoleById(Long id);

    Page<RoleResponseDto> getAllRoles(int page, int size, String name);

    RoleResponseDto updateRole(Long id, RoleRequestDto requestDto);

    void deleteRole(Long id);
}