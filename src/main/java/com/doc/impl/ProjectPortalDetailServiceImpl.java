package com.doc.impl;

import com.doc.dto.project.portal.*;
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

import java.util.Date;
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
            throw new ValidationException("Portal '" + dto.getPortalName() + "' already exists", "ERR_DUPLICATE_PORTAL");
        }

        User user = getUser(userId);

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

        // Auto-approve only if user is Admin or Operation Head
        if (isAdminOrOpHead(user)) {
            entity.setStatus("APPROVED");
            entity.setApprovedBy(user);
            entity.setApprovalDate(new Date());
        } else {
            entity.setStatus("PENDING");
        }

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
                .orElseThrow(() -> new ResourceNotFoundException("Portal detail not found", "ERR_PORTAL_NOT_FOUND"));

        if (!entity.getProject().getId().equals(projectId)) {
            throw new ValidationException("Portal does not belong to this project", "ERR_INVALID_PROJECT");
        }

        User user = getUser(userId);

        entity.setPortalName(dto.getPortalName().trim());
        entity.setPortalUrl(dto.getPortalUrl());
        entity.setUsername(dto.getUsername().trim());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        entity.setRemarks(dto.getRemarks());
        entity.setUpdatedBy(user);

        // If not Admin/OpHead → force back to PENDING on edit
        if (!isAdminOrOpHead(user)) {
            entity.setStatus("PENDING");
            entity.setApprovedBy(null);
            entity.setApprovalDate(null);
            entity.setApprovalRemarks(null);
        }

        entity = portalDetailRepo.save(entity);
        return mapToResponseDto(entity);
    }

    @Override
    public void deletePortalDetail(Long projectId, Long detailId, Long userId) {
        getProjectAndCheckAccess(projectId, userId);

        ProjectPortalDetail entity = portalDetailRepo.findByIdAndIsDeletedFalse(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Portal detail not found", "ERR_PORTAL_NOT_FOUND"));

        if (!entity.getProject().getId().equals(projectId)) {
            throw new ValidationException("Invalid project", "ERR_INVALID_PROJECT");
        }

        entity.setDeleted(true);
        portalDetailRepo.save(entity);
    }

    @Override
    public ProjectPortalDetailResponseDto approveOrRejectPortalDetail(
            Long projectId, Long detailId, Long userId, ProjectPortalDetailApprovalDto approvalDto) {

        Project project = getProjectAndCheckAccess(projectId, userId);

        ProjectPortalDetail entity = portalDetailRepo.findByIdAndIsDeletedFalse(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Portal detail not found", "ERR_PORTAL_NOT_FOUND"));

        if (!entity.getProject().getId().equals(projectId)) {
            throw new ValidationException("Invalid project", "ERR_INVALID_PROJECT");
        }

        if (!"PENDING".equals(entity.getStatus())) {
            throw new ValidationException("Only PENDING entries can be approved/rejected", "ERR_NOT_PENDING");
        }

        User approver = getUser(userId);

        if (!isAdminOrOpHead(approver) && !approver.isManagerFlag()) {
            throw new ValidationException("Unauthorized: Only Admin, Operation Head or Manager can approve", "ERR_UNAUTHORIZED_APPROVAL");
        }

        String action = approvalDto.getStatus();
        if (!"APPROVED".equals(action) && !"REJECTED".equals(action)) {
            throw new ValidationException("Status must be APPROVED or REJECTED", "ERR_INVALID_STATUS");
        }

        entity.setStatus(action);
        entity.setApprovedBy(approver);
        entity.setApprovalDate(new Date());
        entity.setApprovalRemarks(approvalDto.getApprovalRemarks());
        entity.setUpdatedBy(approver);

        entity = portalDetailRepo.save(entity);
        return mapToResponseDto(entity);
    }

    // Helper Methods
    private User getUser(Long userId) {
        return userRepo.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));
    }



    private boolean isAdminOrOpHead(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()) || "OPERATION_HEAD".equals(r.getName()));
    }

    private Project getProjectAndCheckAccess(Long projectId, Long userId) {
        Project project = projectRepo.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));

        User user = getUser(userId);

        if (isAdminOrOpHead(user)) {
            return project;
        }

        boolean isAssigned = assignmentRepo
                .findByProjectIdAndAssignedUserIdAndIsDeletedFalse(projectId, userId)
                .isPresent();

        if (!isAssigned) {
            throw new ValidationException("Access denied to this project", "ERR_UNAUTHORIZED_PORTAL_ACCESS");
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
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setCreatedByName(entity.getCreatedBy() != null ? entity.getCreatedBy().getFullName() : null);
        dto.setUpdatedDate(entity.getUpdatedDate());
        dto.setUpdatedByName(entity.getUpdatedBy() != null ? entity.getUpdatedBy().getFullName() : null);

        dto.setStatus(entity.getStatus());
        dto.setApprovedByName(entity.getApprovedBy() != null ? entity.getApprovedBy().getFullName() : null);
        dto.setApprovalDate(entity.getApprovalDate());
        dto.setApprovalRemarks(entity.getApprovalRemarks());

        return dto;
    }
}