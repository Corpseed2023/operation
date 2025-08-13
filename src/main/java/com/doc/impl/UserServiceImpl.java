package com.doc.impl;

import com.doc.dto.user.UserRequestDto;
import com.doc.dto.user.UserResponseDto;
import com.doc.entity.user.Department;
import com.doc.entity.user.Designation;
import com.doc.entity.user.Role;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.DepartmentRepository;
import com.doc.repository.DesignationRepository;
import com.doc.repository.RoleRepository;
import com.doc.repository.UserRepository;
import com.doc.service.UserService;
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


@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DesignationRepository designationRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public UserResponseDto createUser(UserRequestDto requestDto) {
        logger.info("Creating user with email: {}, roleIds: {}", requestDto.getEmail(), requestDto.getRoleIds());
        validateRequestDto(requestDto);

        if (userRepository.existsByEmailAndIsDeletedFalse(requestDto.getEmail().trim())) {
            throw new ValidationException("User with email " + requestDto.getEmail() + " already exists");
        }

        Designation designation = validateDesignation(requestDto.getDesignationId());
        List<Department> departments = validateDepartments(requestDto.getDepartmentIds(), designation.getDepartment().getId());
        List<Role> roles = validateRoles(requestDto.getRoleIds());
        User manager = validateManager(requestDto.getManagerId(), roles);

        User user = new User();
        mapRequestDtoToEntity(user, requestDto);
        user.setCreatedDate(new Date());
        user.setUpdatedDate(new Date());
        user.setDeleted(false);
        user.setUserDesignation(designation);
        user.setDepartments(departments);
        user.setRoles(roles);
        user.setManager(manager);

        user = userRepository.save(user);
        return mapToResponseDto(user);
    }

    @Override
    public UserResponseDto getUserById(Long id) {
        logger.info("Fetching user with ID: {}", id);
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found"));
        return mapToResponseDto(user);
    }

    @Override
    public List<UserResponseDto> getAllUsers(int page, int size, String fullName, String email, Boolean managerFlag) {
        logger.info("Fetching users, page: {}, size: {}, fullName: {}, email: {}, managerFlag: {}",
                page, size, fullName, email, managerFlag);
        PageRequest pageable = PageRequest.of(page, size);
        Page<User> userPage;

        if (fullName != null || email != null || managerFlag != null) {
            userPage = userRepository.findByFilters(fullName, email, managerFlag, pageable);
        } else {
            userPage = userRepository.findByIsDeletedFalse(pageable);
        }

        return userPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDto updateUser(Long id, UserRequestDto requestDto) {
        logger.info("Updating user with ID: {}, email: {}, roleIds: {}", id, requestDto.getEmail(), requestDto.getRoleIds());
        validateRequestDto(requestDto);

        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found"));

        if (!user.getEmail().equals(requestDto.getEmail().trim()) &&
                userRepository.existsByEmailAndIsDeletedFalse(requestDto.getEmail().trim())) {
            throw new ValidationException("User with email " + requestDto.getEmail() + " already exists");
        }

        Designation designation = validateDesignation(requestDto.getDesignationId());
        List<Department> departments = validateDepartments(requestDto.getDepartmentIds(), designation.getDepartment().getId());
        List<Role> roles = validateRoles(requestDto.getRoleIds());
        User manager = validateManager(requestDto.getManagerId(), roles);

        mapRequestDtoToEntity(user, requestDto);
        user.setUpdatedDate(new Date());
        user.setUserDesignation(designation);
        user.setDepartments(departments);
        user.setRoles(roles);
        user.setManager(manager);

        user = userRepository.save(user);
        return mapToResponseDto(user);
    }

    @Override
    public void deleteUser(Long id) {
        logger.info("Deleting user with ID: {}", id);
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found"));
        user.setDeleted(true);
        user.setUpdatedDate(new Date());
        userRepository.save(user);
    }

    private void validateRequestDto(UserRequestDto requestDto) {
        if (requestDto.getFullName() == null || requestDto.getFullName().trim().isEmpty()) {
            throw new ValidationException("Full name cannot be empty");
        }
        if (requestDto.getEmail() == null || requestDto.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email cannot be empty");
        }
        if (requestDto.getDesignationId() == null) {
            throw new ValidationException("Designation ID cannot be null");
        }
        if (requestDto.getDepartmentIds() == null || requestDto.getDepartmentIds().isEmpty()) {
            throw new ValidationException("Department IDs cannot be null or empty");
        }
        if (requestDto.getRoleIds() == null || requestDto.getRoleIds().isEmpty()) {
            throw new ValidationException("Role IDs cannot be null or empty");
        }
    }

    private Designation validateDesignation(Long designationId) {
        return designationRepository.findById(designationId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Designation with ID " + designationId + " not found"));
    }

    private List<Department> validateDepartments(List<Long> departmentIds, Long designationDepartmentId) {
        List<Department> departments = departmentRepository.findAllById(departmentIds)
                .stream()
                .filter(d -> !d.isDeleted())
                .collect(Collectors.toList());
        if (departments.size() != departmentIds.size()) {
            throw new ResourceNotFoundException("One or more departments not found");
        }
        // Optional: Enforce that designation's department is included in departmentIds
        if (!departmentIds.contains(designationDepartmentId)) {
            throw new ValidationException("Designation's department ID " + designationDepartmentId + " must be included in departmentIds");
        }
        return departments;
    }

    private List<Role> validateRoles(List<Long> roleIds) {
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new ResourceNotFoundException("One or more roles not found");
        }
        return roles;
    }

    private User validateManager(Long managerId, List<Role> roles) {
        boolean hasAdminRole = roles.stream().anyMatch(role -> role.getName().equalsIgnoreCase("ADMIN"));
        if (!hasAdminRole && managerId == null) {
            throw new ValidationException("Manager ID is required for non-ADMIN roles");
        }
        if (managerId != null) {
            return userRepository.findByIdAndIsDeletedFalse(managerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager with ID " + managerId + " not found"));
        }
        return null;
    }

    private void mapRequestDtoToEntity(User user, UserRequestDto requestDto) {
        user.setFullName(requestDto.getFullName().trim());
        user.setEmail(requestDto.getEmail().trim());
        user.setContactNo(requestDto.getContactNo() != null ? requestDto.getContactNo().trim() : null);
        user.setManagerFlag(requestDto.getManagerFlag() != null ? requestDto.getManagerFlag() : false);
    }

    private UserResponseDto mapToResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setContactNo(user.getContactNo());
        // Use userDesignation instead of designation
        dto.setDesignation(user.getUserDesignation() != null ? user.getUserDesignation().getName() : null);
        dto.setDesignationId(user.getUserDesignation() != null ? user.getUserDesignation().getId() : null);
        dto.setDepartmentIds(user.getDepartments().stream()
                .map(Department::getId)
                .collect(Collectors.toList()));
        dto.setRoleIds(user.getRoles().stream()
                .map(Role::getId)
                .collect(Collectors.toList()));
        dto.setManagerId(user.getManager() != null ? user.getManager().getId() : null);
        dto.setManagerFlag(user.isManagerFlag());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setUpdatedDate(user.getUpdatedDate());
        return dto;
    }
}