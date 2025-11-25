package com.doc.impl;

import com.doc.dto.project.ProjectResponseDto;
import com.doc.entity.project.Project;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.service.ProjectSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectSearchServiceImpl implements ProjectSearchService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private List<Long> getAccessibleUserIds(User user) {
        List<Long> userIds = new ArrayList<>();
        userIds.add(user.getId());
        if (user.isManagerFlag()) {
            List<User> subordinates = userRepository.findByManagerIdAndIsDeletedFalse(user.getId());
            userIds.addAll(subordinates.stream().map(User::getId).collect(Collectors.toList()));
        }
        return userIds;
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByCompanyName(String companyName, Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or deleted", "ERR_USER_NOT_FOUND"));
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findByCompanyNameContainingAndIsDeletedFalse(companyName);
        } else {
            List<Long> accessibleUserIds = getAccessibleUserIds(user);
            projects = projectRepository.findByCompanyNameContainingAndAssignedUserIdsAndIsDeletedFalse(companyName, accessibleUserIds);
        }
        return projects.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByProjectNumber(String projectNumber, Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or deleted", "ERR_USER_NOT_FOUND"));
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findByProjectNoContainingAndIsDeletedFalse(projectNumber);
        } else {
            List<Long> accessibleUserIds = getAccessibleUserIds(user);
            projects = projectRepository.findByProjectNoContainingAndAssignedUserIdsAndIsDeletedFalse(projectNumber, accessibleUserIds);
        }
        return projects.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByContactName(String contactName, Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or deleted", "ERR_USER_NOT_FOUND"));
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findByContactNameContainingAndIsDeletedFalse(contactName);
        } else {
            List<Long> accessibleUserIds = getAccessibleUserIds(user);
            projects = projectRepository.findByContactNameContainingAndAssignedUserIdsAndIsDeletedFalse(contactName, accessibleUserIds);
        }
        return projects.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByProjectName(String projectName, Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or deleted", "ERR_USER_NOT_FOUND"));
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findByNameContainingAndIsDeletedFalse(projectName);
        } else {
            List<Long> accessibleUserIds = getAccessibleUserIds(user);
            projects = projectRepository.findByNameContainingAndAssignedUserIdsAndIsDeletedFalse(projectName, accessibleUserIds);
        }
        return projects.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Maps Project entity to ProjectResponseDto
     * Uses salesPersonId and salesPersonName directly from Project (from microservice)
     */
    private ProjectResponseDto mapToResponseDto(Project project) {
        ProjectResponseDto dto = new ProjectResponseDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setProjectNo(project.getProjectNo());
        dto.setUnbilledNumber(project.getUnbilledNumber());
        dto.setEstimateNumber(project.getEstimateNumber());

        // FROM MICROSERVICE
        dto.setSalesPersonId(project.getSalesPersonId());
        dto.setSalesPersonName(project.getSalesPersonName());

        dto.setProductId(project.getProduct() != null ? project.getProduct().getId() : null);
        dto.setCompanyId(project.getCompany() != null ? project.getCompany().getId() : null);
        dto.setCompanyName(project.getCompany() != null ? project.getCompany().getName() : null);
        dto.setContactId(project.getContact() != null ? project.getContact().getId() : null);
        dto.setContactName(project.getContact() != null ? project.getContact().getName() : null);

        dto.setLeadId(project.getLeadId());
        dto.setDate(project.getDate());
        dto.setAddress(project.getAddress());
        dto.setCity(project.getCity());
        dto.setState(project.getState());
        dto.setCountry(project.getCountry());
        dto.setPrimaryPinCode(project.getPrimaryPinCode());

        dto.setTotalAmount(project.getPaymentDetail() != null ? project.getPaymentDetail().getTotalAmount() : 0.0);
        dto.setDueAmount(project.getPaymentDetail() != null ? project.getPaymentDetail().getDueAmount() : 0.0);
        dto.setPaymentTypeId(project.getPaymentDetail() != null && project.getPaymentDetail().getPaymentType() != null
                ? project.getPaymentDetail().getPaymentType().getId() : null);
        dto.setApprovedById(project.getPaymentDetail() != null && project.getPaymentDetail().getApprovedBy() != null
                ? project.getPaymentDetail().getApprovedBy().getId() : null);

        dto.setCreatedDate(project.getCreatedDate());
        dto.setUpdatedDate(project.getUpdatedDate());
        dto.setDeleted(project.isDeleted());
        dto.setActive(project.isActive());

        return dto;
    }
}