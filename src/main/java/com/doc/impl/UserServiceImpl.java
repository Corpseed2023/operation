        package com.doc.impl;

import com.doc.dto.user.UserRequestDto;
import com.doc.dto.user.UserResponseDto;
import com.doc.entity.user.Department;
import com.doc.entity.user.Designation;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repsoitory.DepartmentRepository;
import com.doc.repsoitory.DesignationRepository;
import com.doc.repsoitory.UserRepository;
import com.doc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DesignationRepository designationRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public UserResponseDto createUser(UserRequestDto requestDto) {
        validateRequestDto(requestDto);

        if (userRepository.existsByEmailAndIsDeletedFalse(requestDto.getEmail().trim())) {
            throw new ValidationException("User with email " + requestDto.getEmail() + " already exists");
        }

        // Validate designation name and department
        Designation designation = validateDesignation(requestDto.getDesignationId(), requestDto.getDesignation(), requestDto.getDepartmentId());

        // Validate department IDs
        List<Department> departments = new ArrayList<>();
        if (requestDto.getDepartmentIds() != null && !requestDto.getDepartmentIds().isEmpty()) {
            departments = departmentRepository.findAllById(requestDto.getDepartmentIds())
                    .stream()
                    .filter(d -> !d.isDeleted())
                    .collect(Collectors.toList());
            if (departments.size() != requestDto.getDepartmentIds().size()) {
                throw new ResourceNotFoundException("One or more departments not found");
            }
            // Ensure the designation's department is included in departmentIds
            if (!requestDto.getDepartmentIds().contains(requestDto.getDepartmentId())) {
                throw new ValidationException("Designation's department ID " + requestDto.getDepartmentId() + " must be included in departmentIds");
            }
        } else {
            // If no departmentIds provided, use the designation's department
            departments.add(designation.getDepartment());
        }

        User user = new User();
        mapRequestDtoToEntity(user, requestDto);
        user.setCreatedDate(new Date());
        user.setUpdatedDate(new Date());
        user.setDeleted(false);
        user.setUserDesignation(designation);
        user.setDepartments(departments);

        user = userRepository.save(user);
        return mapToResponseDto(user);
    }

    @Override
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found"));
        return mapToResponseDto(user);
    }

    @Override
    public List<UserResponseDto> getAllUsers(int page, int size, String fullName, String email, Boolean isManager) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<User> userPage;

        if (fullName != null || email != null || isManager != null) {
            userPage = userRepository.findByFilters(fullName, email, isManager, pageable);
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
        validateRequestDto(requestDto);

        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found"));

        if (!user.getEmail().equals(requestDto.getEmail().trim()) &&
                userRepository.existsByEmailAndIsDeletedFalse(requestDto.getEmail().trim())) {
            throw new ValidationException("User with email " + requestDto.getEmail() + " already exists");
        }

        // Validate designation name and department
        Designation designation = validateDesignation(requestDto.getDesignationId(), requestDto.getDesignation(), requestDto.getDepartmentId());

        // Validate department IDs
        List<Department> departments = new ArrayList<>();
        if (requestDto.getDepartmentIds() != null && !requestDto.getDepartmentIds().isEmpty()) {
            departments = departmentRepository.findAllById(requestDto.getDepartmentIds())
                    .stream()
                    .filter(d -> !d.isDeleted())
                    .collect(Collectors.toList());
            if (departments.size() != requestDto.getDepartmentIds().size()) {
                throw new ResourceNotFoundException("One or more departments not found");
            }
            if (!requestDto.getDepartmentIds().contains(requestDto.getDepartmentId())) {
                throw new ValidationException("Designation's department ID " + requestDto.getDepartmentId() + " must be included in departmentIds");
            }
        } else {
            departments.add(designation.getDepartment());
        }

        mapRequestDtoToEntity(user, requestDto);
        user.setUpdatedDate(new Date());
        user.setUserDesignation(designation);
        user.setDepartments(departments);

        user = userRepository.save(user);
        return mapToResponseDto(user);
    }

    @Override
    public void deleteUser(Long id) {
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
        if (requestDto.getDesignation() == null || requestDto.getDesignation().trim().isEmpty()) {
            throw new ValidationException("Designation name cannot be empty");
        }
        if (requestDto.getDesignationId() == null) {
            throw new ValidationException("Designation ID cannot be null");
        }
        if (requestDto.getDepartmentId() == null) {
            throw new ValidationException("Department ID for designation cannot be null");
        }
        if (requestDto.getDepartmentIds() == null) {
            throw new ValidationException("Department IDs cannot be null");
        }
    }

    private Designation validateDesignation(Long designationId, String designationName, Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department with ID " + departmentId + " not found"));

        Designation designation = designationRepository.findById(designationId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Designation with ID " + designationId + " not found"));

        if (!designation.getName().equalsIgnoreCase(designationName.trim())) {
            throw new ValidationException("Designation name " + designationName + " does not match ID " + designationId);
        }
        if (!designation.getDepartment().getId().equals(departmentId)) {
            throw new ValidationException("Designation with ID " + designationId + " does not belong to department ID " + departmentId);
        }

        return designation;
    }

    private void mapRequestDtoToEntity(User user, UserRequestDto requestDto) {
        user.setFullName(requestDto.getFullName().trim());
        user.setEmail(requestDto.getEmail().trim());
        user.setContactNo(requestDto.getContactNo() != null ? requestDto.getContactNo().trim() : null);
        user.setDesignation(requestDto.getDesignation().trim());
        user.setManager(requestDto.getIsManager() != null ? requestDto.getIsManager() : false);
    }

    private UserResponseDto mapToResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setContactNo(user.getContactNo());
        dto.setDesignation(user.getDesignation());

        dto.setDesignationId(user.getUserDesignation() != null ? user.getUserDesignation().getId() : null);


        dto.setManager(user.isManager());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setUpdatedDate(user.getUpdatedDate());
        return dto;
    }
}
