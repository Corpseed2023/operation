package com.doc.impl;


import com.doc.dto.project.portal.ProjectPortalDetailListResponseDto;
import com.doc.dto.project.portal.ProjectPortalDetailRequestDto;
import com.doc.dto.project.portal.ProjectPortalDetailResponseDto;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectPortalDetail;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.repository.ProjectRepository;
import com.doc.repository.projectRepo.ProjectPortalDetailRepository;
import com.doc.repository.UserRepository;
import com.doc.service.ProjectPortalDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProjectPortalDetailServiceImpl implements ProjectPortalDetailService {

    @Autowired private ProjectPortalDetailRepository portalDetailRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ProjectMilestoneAssignmentRepository assignmentRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public ProjectPortalDetailResponseDto addPortalDetail(Long projectId, Long userId, ProjectPortalDetailRequestDto dto) {
        Project project = getProjectAndCheckAccess(projectId, userId);

        if (portalDetailRepo.existsByProjectIdAndPortalNameAndIsDeletedFalse(projectId, dto.getPortalName().trim())) {
            throw new ValidationException("Portal '" + dto.getPortalName() + "' already exists for this project", "ERR_DUPLICATE_PORTAL");
        }

        User user = userRepo.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        ProjectPortalDetail entity = new ProjectPortalDetail();
        entity.setProject(project);
        entity.setCompany(project.getCompany());
        entity.setPortalName(dto.getPortalName().trim());
        entity.setPortalUrl(dto.getPortalUrl());
        entity.setUsername(dto.getUsername().trim());
        entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        entity.setRemarks(dto.getRemarks());
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);

        entity = portalDetailRepo.save(entity);
        return mapToResponseDto(entity);
    }

    @Override
    public ProjectPortalDetailListResponseDto getPortalDetails(Long projectId, Long userId) {
        Project project = getProjectAndCheckAccess(projectId, userId);

        List<ProjectPortalDetail> details = portalDetailRepo
                .findByProjectIdAndIsDeletedFalseOrderByCreatedDateDesc(projectId);

        List<ProjectPortalDetailResponseDto> dtos = details.stream()
                .map(this::mapToResponseDto)
                .toList();

        ProjectPortalDetailListResponseDto response = new ProjectPortalDetailListResponseDto();
        response.setProjectId(project.getId());
        response.setProjectNo(project.getProjectNo());
        response.setCompanyName(project.getCompany().getName());
        response.setPortals(dtos);
        return response;
    }

    @Override
    public ProjectPortalDetailResponseDto updatePortalDetail(Long projectId, Long detailId, Long userId, ProjectPortalDetailRequestDto dto) {
        getProjectAndCheckAccess(projectId, userId);

        ProjectPortalDetail entity = portalDetailRepo.findByIdAndIsDeletedFalse(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Portal detail not found", "ERR_PORTAL_DETAIL_NOT_FOUND"));

        if (!entity.getProject().getId().equals(projectId)) {
            throw new ValidationException("Portal detail does not belong to this project", "ERR_INVALID_PROJECT");
        }

        User user = userRepo.findActiveUserById(userId).orElseThrow();

        entity.setPortalName(dto.getPortalName().trim());
        entity.setPortalUrl(dto.getPortalUrl());
        entity.setUsername(dto.getUsername().trim());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        entity.setRemarks(dto.getRemarks());
        entity.setUpdatedBy(user);

        entity = portalDetailRepo.save(entity);
        return mapToResponseDto(entity);
    }

    @Override
    public void deletePortalDetail(Long projectId, Long detailId, Long userId) {
        getProjectAndCheckAccess(projectId, userId);

        ProjectPortalDetail entity = portalDetailRepo.findByIdAndIsDeletedFalse(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Portal detail not found", "ERR_PORTAL_DETAIL_NOT_FOUND"));

        if (!entity.getProject().getId().equals(projectId)) {
            throw new ValidationException("Invalid project", "ERR_INVALID_PROJECT");
        }

        entity.setDeleted(true);
        portalDetailRepo.save(entity);
    }

    // Reusable access check
    private Project getProjectAndCheckAccess(Long projectId, Long userId) {
        Project project = projectRepo.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));

        User user = userRepo.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        boolean isOpHead = user.getRoles().stream().anyMatch(r -> "OPERATION_HEAD".equals(r.getName()));

        if (isAdmin || isOpHead) return project;

        boolean isAssigned = assignmentRepo
                .findByProjectIdAndAssignedUserIdAndIsDeletedFalse(projectId, userId)
                .isPresent();

        if (!isAssigned) {
            throw new ValidationException("You are not authorized to manage portal details for this project",
                    "ERR_UNAUTHORIZED_PORTAL_ACCESS");
        }
        return project;
    }

    private ProjectPortalDetailResponseDto mapToResponseDto(ProjectPortalDetail entity) {
        ProjectPortalDetailResponseDto dto = new ProjectPortalDetailResponseDto();
        dto.setId(entity.getId());
        dto.setProjectId(entity.getProject().getId());
        dto.setPortalName(entity.getPortalName());
        dto.setPortalUrl(entity.getPortalUrl());
        dto.setUsername(entity.getUsername());
        dto.setPassword("••••••••"); // Always masked
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setCreatedByName(entity.getCreatedBy() != null ? entity.getCreatedBy().getFullName() : null);
        dto.setUpdatedDate(entity.getUpdatedDate());
        dto.setUpdatedByName(entity.getUpdatedBy() != null ? entity.getUpdatedBy().getFullName() : null);
        return dto;
    }
}