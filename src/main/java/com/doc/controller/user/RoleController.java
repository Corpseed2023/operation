package com.doc.controller.user;

import com.doc.dto.role.RoleRequestDto;
import com.doc.dto.role.RoleResponseDto;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @PostMapping
    public ResponseEntity<?> createRole(@Valid @RequestBody RoleRequestDto requestDto) {
        try {
            RoleResponseDto responseDto = roleService.createRole(requestDto);
            return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
        } catch (ValidationException ex) {
            throw new ValidationException(ex.getMessage(), "ERR_DUPLICATE_ROLE");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create role", ex);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleById(@PathVariable Long id) {
        try {
            RoleResponseDto responseDto = roleService.getRoleById(id);
            return ResponseEntity.ok(responseDto);
        } catch (ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("Role with ID " + id + " not found", "ERR_ROLE_NOT_FOUND");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to retrieve role", ex);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        try {
            if (page < 0 || size <= 0) {
                throw new ValidationException("Page must be non-negative and size must be positive", "ERR_INVALID_PAGINATION");
            }
            Page<RoleResponseDto> roles = roleService.getAllRoles(page, size, name);
            if (roles.isEmpty() && name != null) {
                throw new ResourceNotFoundException("No roles found with name containing: " + name, "ERR_ROLES_NOT_FOUND");
            }
            return ResponseEntity.ok(roles);
        } catch (ValidationException ex) {
            throw new ValidationException(ex.getMessage(), ex.getErrorCode());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to retrieve roles", ex);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequestDto requestDto) {
        try {
            RoleResponseDto responseDto = roleService.updateRole(id, requestDto);
            return ResponseEntity.ok(responseDto);
        } catch (ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("Role with ID " + id + " not found", "ERR_ROLE_NOT_FOUND");
        } catch (ValidationException ex) {
            throw new ValidationException(ex.getMessage(), "ERR_DUPLICATE_ROLE");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to update role", ex);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("Role with ID " + id + " not found", "ERR_ROLE_NOT_FOUND");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete role", ex);
        }
    }
}