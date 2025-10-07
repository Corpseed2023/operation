package com.doc.impl;

import com.doc.dto.user.UserRequestDto;
import com.doc.dto.user.UserResponseDto;
import com.doc.entity.department.Department;
import com.doc.entity.department.Designation;
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

import java.time.LocalDate;
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
        logger.info("Creating user with ID: {}, email: {}, roleIds: {}", requestDto.getId(), requestDto.getEmail(), requestDto.getRoleIds());
        validateRequestDto(requestDto);

        if (userRepository.existsById(requestDto.getId())) {
            throw new ValidationException("User with ID " + requestDto.getId() + " already exists", "DUPLICATE_USER_ID");
        }

        if (userRepository.existsByEmailAndIsActiveTrueAndIsDeletedFalse(requestDto.getEmail().trim())) {
            throw new ValidationException("User with email " + requestDto.getEmail() + " already exists", "DUPLICATE_EMAIL");
        }

        Designation designation = validateDesignation(requestDto.getDesignationId());
        List<Department> departments = validateDepartments(requestDto.getDepartmentIds(), designation.getDepartment().getId());
        List<Role> roles = validateRoles(requestDto.getRoleIds());
        User manager = validateManager(requestDto.getManagerId(), roles);

        User user = new User();
        user.setId(requestDto.getId());
        mapRequestDtoToEntity(user, requestDto);
        user.setCreatedDate(new Date());
        user.setUpdatedDate(new Date());
        user.setDeleted(false);
        user.setActive(true);
        user.setUserDesignation(designation);
        user.setDepartments(departments);
        user.setRoles(roles);
        user.setManager(manager);
        user.setDate(LocalDate.now());

        user = userRepository.save(user);
        return mapToResponseDto(user);
    }

    @Override
    public UserResponseDto getUserById(Long id) {
        logger.info("Fetching user with ID: {}", id);
        User user = userRepository.findActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found", "USER_NOT_FOUND"));
        return mapToResponseDto(user);
    }

    @Override
    public List<UserResponseDto> getAllUsers(int page, int size, Long userId) {
        logger.info("Fetching users for userId: {}, page: {}, size: {}", userId, page, size);
        User requestingUser = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Requesting user with ID " + userId + " not found", "USER_NOT_FOUND"));

        PageRequest pageable = PageRequest.of(page, size);
        Page<User> userPage;

        boolean isAdminOrOpHead = requestingUser.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("ADMIN") || role.getName().equalsIgnoreCase("Operation Head"));

        if (isAdminOrOpHead) {
            logger.info("User {} has ADMIN or Operation Head role, fetching all users", userId);
            userPage = userRepository.findByIsActiveTrueAndIsDeletedFalse(pageable);
        } else if (requestingUser.isManagerFlag()) {
            logger.info("User {} is a manager, fetching managed users", userId);
            userPage = userRepository.findByManagerIdAndIsDeletedFalseList(userId, pageable);
        } else {
            logger.warn("User {} does not have sufficient permissions", userId);
            throw new ValidationException("User does not have permission to view users", "UNAUTHORIZED_ACCESS");
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

        User user = userRepository.findActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found", "USER_NOT_FOUND"));

        if (!user.getEmail().equals(requestDto.getEmail().trim()) &&
                userRepository.existsByEmailAndIsActiveTrueAndIsDeletedFalse(requestDto.getEmail().trim())) {
            throw new ValidationException("User with email " + requestDto.getEmail() + " already exists", "DUPLICATE_EMAIL");
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
        User user = userRepository.findActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found", "USER_NOT_FOUND"));
        user.setDeleted(true);
        user.setUpdatedDate(new Date());
        userRepository.save(user);
    }

    private void validateRequestDto(UserRequestDto requestDto) {
        if (requestDto.getId() == null) {
            throw new ValidationException("User ID cannot be null", "INVALID_USER_ID");
        }
        if (requestDto.getFullName() == null || requestDto.getFullName().trim().isEmpty()) {
            throw new ValidationException("Full name cannot be empty", "INVALID_FULL_NAME");
        }
        if (requestDto.getEmail() == null || requestDto.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email cannot be empty", "INVALID_EMAIL");
        }
        if (requestDto.getDesignationId() == null) {
            throw new ValidationException("Designation ID cannot be null", "INVALID_DESIGNATION_ID");
        }
        if (requestDto.getDepartmentIds() == null || requestDto.getDepartmentIds().isEmpty()) {
            throw new ValidationException("Department IDs cannot be null or empty", "INVALID_DEPARTMENT_IDS");
        }
        if (requestDto.getRoleIds() == null || requestDto.getRoleIds().isEmpty()) {
            throw new ValidationException("Role IDs cannot be null or empty", "INVALID_ROLE_IDS");
        }
    }

    private Designation validateDesignation(Long designationId) {
        return designationRepository.findById(designationId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Designation with ID " + designationId + " not found", "DESIGNATION_NOT_FOUND"));
    }

    private List<Department> validateDepartments(List<Long> departmentIds, Long designationDepartmentId) {
        List<Department> departments = departmentRepository.findAllById(departmentIds)
                .stream()
                .filter(d -> !d.isDeleted())
                .collect(Collectors.toList());
        if (departments.size() != departmentIds.size()) {
            throw new ResourceNotFoundException("One or more departments not found", "DEPARTMENT_NOT_FOUND");
        }
        if (!departmentIds.contains(designationDepartmentId)) {
            throw new ValidationException("Designation's department ID " + designationDepartmentId + " must be included in departmentIds", "INVALID_DEPARTMENT_DESIGNATION");
        }
        return departments;
    }

    private List<Role> validateRoles(List<Long> roleIds) {
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new ResourceNotFoundException("One or more roles not found", "ROLE_NOT_FOUND");
        }
        return roles;
    }

    private User validateManager(Long managerId, List<Role> roles) {
        boolean hasAdminRole = roles.stream().anyMatch(role -> role.getName().equalsIgnoreCase("ADMIN"));
        if (!hasAdminRole && managerId == null) {
            throw new ValidationException("Manager ID is required for non-ADMIN roles", "INVALID_MANAGER_ID");
        }
        if (managerId != null) {
            return userRepository.findActiveUserById(managerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager with ID " + managerId + " not found", "MANAGER_NOT_FOUND"));
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