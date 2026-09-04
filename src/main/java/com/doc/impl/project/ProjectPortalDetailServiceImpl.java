package com.doc.impl.project;

import com.doc.dto.project.portal.ProjectPortalDetailApprovalDto;
import com.doc.dto.project.portal.ProjectPortalDetailListResponseDto;
import com.doc.dto.project.portal.ProjectPortalDetailRequestDto;
import com.doc.dto.project.portal.ProjectPortalDetailResponseDto;
import com.doc.entity.department.Department;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.project.ProjectPortalDetail;
import com.doc.entity.project.ProjectPortalDetailStatus;
import com.doc.entity.user.Role;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.projectRepo.ProjectPortalDetailRepository;
import com.doc.service.project.ProjectPortalDetailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@Log4j2
public class ProjectPortalDetailServiceImpl
        implements ProjectPortalDetailService {

    private static final Set<String> TECHNICAL_DEPARTMENT_NAMES = Set.of(
            "TECHNICAL",
            "TECHNICAL_DEPARTMENT"
    );

    private static final Set<String> ADMIN_OPERATION_HEAD_ROLES = Set.of(
            "ADMIN",
            "ROLE_ADMIN",
            "OPERATION_HEAD",
            "ROLE_OPERATION_HEAD"
    );

    private final ProjectPortalDetailRepository portalDetailRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final ProjectMilestoneAssignmentRepository assignmentRepo;
    private final PasswordEncoder passwordEncoder;

    public ProjectPortalDetailServiceImpl(
            ProjectPortalDetailRepository portalDetailRepo,
            ProjectRepository projectRepo,
            UserRepository userRepo,
            ProjectMilestoneAssignmentRepository assignmentRepo,
            PasswordEncoder passwordEncoder
    ) {
        this.portalDetailRepo = portalDetailRepo;
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
        this.assignmentRepo = assignmentRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ProjectPortalDetailResponseDto addPortalDetail(
            Long projectId,
            Long userId,
            ProjectPortalDetailRequestDto dto
    ) {
        log.info(
                "Adding portal detail: projectId={}, userId={}",
                projectId,
                userId
        );

        Project project = getProjectAndCheckAccess(
                projectId,
                userId
        );

        User user = getUser(userId);

        /*
         * Admin and Operation Head retain override access.
         * Other users must belong to the Technical department and
         * must be assigned to this project.
         */
        if (!isAdminOrOperationHead(user)) {
            validateTechnicalDepartmentUser(user);

            if (!isUserAssignedToProject(projectId, userId)) {
                throw new ValidationException(
                        "Only a Technical department user assigned to this "
                                + "project can add portal details",
                        "ERR_PORTAL_CREATE_NOT_ALLOWED"
                );
            }
        }

        validateCreateRequest(dto);

        String portalName = dto.getPortalName().trim();

        if (portalDetailRepo.existsActivePortalName(
                projectId,
                portalName
        )) {
            throw new ValidationException(
                    "Portal '" + portalName + "' already exists",
                    "ERR_DUPLICATE_PORTAL"
            );
        }

        ProjectPortalDetail entity =
                new ProjectPortalDetail();

        entity.setProject(project);
        entity.setCompany(project.getCompany());
        entity.setPortalName(portalName);
        entity.setPortalUrl(
                trimToNull(dto.getPortalUrl())
        );
        entity.setUsername(dto.getUsername().trim());
        entity.setPassword(
                passwordEncoder.encode(
                        dto.getPassword().trim()
                )
        );
        entity.setRemarks(
                trimToNull(dto.getRemarks())
        );
        entity.setDate(LocalDate.now());
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);
        entity.setDeleted(false);

        if (isAdminOrOperationHead(user)) {
            entity.setStatus(
                    ProjectPortalDetailStatus.APPROVED
            );
            entity.setApprovedBy(user);
            entity.setApprovalDate(new Date());
            entity.setApprovalRemarks(
                    "Automatically approved by authorized user"
            );
        } else {
            entity.setStatus(
                    ProjectPortalDetailStatus.PENDING
            );
            entity.setApprovedBy(null);
            entity.setApprovalDate(null);
            entity.setApprovalRemarks(null);
        }

        ProjectPortalDetail saved =
                portalDetailRepo.save(entity);

        log.info(
                "Portal detail created: projectId={}, detailId={}, "
                        + "createdBy={}, status={}",
                projectId,
                saved.getId(),
                userId,
                saved.getStatus()
        );

        return mapToResponseDto(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectPortalDetailListResponseDto getPortalDetails(
            Long projectId,
            Long userId
    ) {
        log.info(
                "Fetching portal details: projectId={}, userId={}",
                projectId,
                userId
        );

        Project project = getProjectAndCheckAccess(
                projectId,
                userId
        );

        User viewer = getUser(userId);

        List<ProjectPortalDetail> details =
                portalDetailRepo.findActiveByProjectId(
                        projectId
                );

        List<ProjectPortalDetailResponseDto> portalDtos =
                details.stream()
                        .map(detail ->
                                mapToResponseDto(
                                        detail,
                                        viewer
                                )
                        )
                        .toList();

        ProjectPortalDetailListResponseDto response =
                new ProjectPortalDetailListResponseDto();

        response.setProjectId(project.getId());
        response.setProjectNo(project.getProjectNo());

        if (project.getCompany() != null) {
            response.setCompanyName(
                    project.getCompany().getName()
            );
        }

        response.setPortals(portalDtos);

        log.info(
                "Portal details fetched: projectId={}, count={}",
                projectId,
                portalDtos.size()
        );

        return response;
    }

    @Override
    public ProjectPortalDetailResponseDto updatePortalDetail(
            Long projectId,
            Long detailId,
            Long userId,
            ProjectPortalDetailRequestDto dto
    ) {
        log.info(
                "Updating portal detail: projectId={}, detailId={}, userId={}",
                projectId,
                detailId,
                userId
        );

        getProjectAndCheckAccess(projectId, userId);

        ProjectPortalDetail entity =
                getPortalDetail(projectId, detailId);

        User user = getUser(userId);

        if (!isAdminOrOperationHead(user)) {
            validateTechnicalDepartmentUser(user);

            if (!isUserAssignedToProject(projectId, userId)) {
                throw new ValidationException(
                        "Only a Technical department user assigned to this "
                                + "project can update portal details",
                        "ERR_PORTAL_UPDATE_NOT_ALLOWED"
                );
            }
        }

        validateUpdateRequest(dto);

        String portalName =
                dto.getPortalName().trim();

        if (portalDetailRepo
                .existsActivePortalNameExcludingId(
                        projectId,
                        portalName,
                        detailId
                )) {

            throw new ValidationException(
                    "Portal '" + portalName + "' already exists",
                    "ERR_DUPLICATE_PORTAL"
            );
        }

        entity.setPortalName(portalName);
        entity.setPortalUrl(
                trimToNull(dto.getPortalUrl())
        );
        entity.setUsername(
                dto.getUsername().trim()
        );
        entity.setRemarks(
                trimToNull(dto.getRemarks())
        );
        entity.setUpdatedBy(user);

        if (StringUtils.hasText(dto.getPassword())) {
            entity.setPassword(
                    passwordEncoder.encode(
                            dto.getPassword().trim()
                    )
            );
        }

        /*
         * Any change made by a normal Technical user requires
         * fresh manager approval.
         */
        if (!isAdminOrOperationHead(user)) {
            entity.setStatus(
                    ProjectPortalDetailStatus.PENDING
            );
            entity.setApprovedBy(null);
            entity.setApprovalDate(null);
            entity.setApprovalRemarks(null);
        }

        ProjectPortalDetail saved =
                portalDetailRepo.save(entity);

        log.info(
                "Portal detail updated: projectId={}, detailId={}, "
                        + "updatedBy={}, status={}",
                projectId,
                detailId,
                userId,
                saved.getStatus()
        );

        return mapToResponseDto(saved, user);
    }

    @Override
    public void deletePortalDetail(
            Long projectId,
            Long detailId,
            Long userId
    ) {
        log.info(
                "Deleting portal detail: projectId={}, detailId={}, userId={}",
                projectId,
                detailId,
                userId
        );

        getProjectAndCheckAccess(projectId, userId);

        ProjectPortalDetail entity =
                getPortalDetail(projectId, detailId);

        User user = getUser(userId);

        if (!isAdminOrOperationHead(user)) {
            validateTechnicalDepartmentUser(user);

            if (!isUserAssignedToProject(projectId, userId)) {
                throw new ValidationException(
                        "Only a Technical department user assigned to this "
                                + "project can delete portal details",
                        "ERR_PORTAL_DELETE_NOT_ALLOWED"
                );
            }
        }

        entity.setDeleted(true);
        entity.setUpdatedBy(user);

        portalDetailRepo.save(entity);

        log.info(
                "Portal detail soft deleted: projectId={}, detailId={}, "
                        + "deletedBy={}",
                projectId,
                detailId,
                userId
        );
    }

    @Override
    public ProjectPortalDetailResponseDto approveOrRejectPortalDetail(
            Long projectId,
            Long detailId,
            Long userId,
            ProjectPortalDetailApprovalDto approvalDto
    ) {
        log.info(
                "Processing portal approval: projectId={}, detailId={}, "
                        + "approverId={}",
                projectId,
                detailId,
                userId
        );

        getProjectAndCheckAccess(projectId, userId);

        ProjectPortalDetail entity =
                getPortalDetail(projectId, detailId);

        if (entity.getStatus()
                != ProjectPortalDetailStatus.PENDING) {

            throw new ValidationException(
                    "Only PENDING portal details can be approved or rejected",
                    "ERR_NOT_PENDING"
            );
        }

        User approver = getUser(userId);

        boolean canApprove =
                isAdminOrOperationHead(approver)
                        || isTechnicalManagerOfSubmitter(
                        approver,
                        entity.getCreatedBy()
                );

        if (!canApprove) {
            log.warn(
                    "Unauthorized portal approval: projectId={}, detailId={}, "
                            + "approverId={}, submittedBy={}",
                    projectId,
                    detailId,
                    userId,
                    entity.getCreatedBy() != null
                            ? entity.getCreatedBy().getId()
                            : null
            );

            throw new ValidationException(
                    "Only the submitter's Technical department manager, "
                            + "Admin, or Operation Head can approve or reject "
                            + "portal details",
                    "ERR_UNAUTHORIZED_APPROVAL"
            );
        }

        if (approvalDto == null) {
            throw new ValidationException(
                    "Approval request is required",
                    "ERR_APPROVAL_REQUEST_REQUIRED"
            );
        }

        /*
         * approvalDto currently returns String.
         * Convert it to ProjectPortalDetailStatus before calling
         * entity.setStatus(...).
         */
        ProjectPortalDetailStatus action =
                parseApprovalStatus(
                        approvalDto.getStatus()
                );

        String approvalRemarks =
                trimToNull(
                        approvalDto.getApprovalRemarks()
                );

        if (action == ProjectPortalDetailStatus.REJECTED
                && !StringUtils.hasText(approvalRemarks)) {

            throw new ValidationException(
                    "Approval remarks are required when rejecting portal details",
                    "ERR_REJECTION_REMARKS_REQUIRED"
            );
        }

        entity.setStatus(action);
        entity.setApprovedBy(approver);
        entity.setApprovalDate(new Date());
        entity.setApprovalRemarks(approvalRemarks);
        entity.setUpdatedBy(approver);

        ProjectPortalDetail saved =
                portalDetailRepo.save(entity);

        log.info(
                "Portal approval completed: projectId={}, detailId={}, "
                        + "approverId={}, status={}",
                projectId,
                detailId,
                userId,
                saved.getStatus()
        );

        return mapToResponseDto(saved, approver);
    }

    private ProjectPortalDetailStatus parseApprovalStatus(
            String status
    ) {
        if (!StringUtils.hasText(status)) {
            throw new ValidationException(
                    "Approval status is required",
                    "ERR_APPROVAL_STATUS_REQUIRED"
            );
        }

        String normalized =
                normalizeName(status);

        try {
            ProjectPortalDetailStatus parsedStatus =
                    ProjectPortalDetailStatus.valueOf(
                            normalized
                    );

            if (parsedStatus
                    != ProjectPortalDetailStatus.APPROVED
                    && parsedStatus
                    != ProjectPortalDetailStatus.REJECTED) {

                throw new ValidationException(
                        "Status must be APPROVED or REJECTED",
                        "ERR_INVALID_STATUS"
                );
            }

            return parsedStatus;

        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "Status must be APPROVED or REJECTED",
                    "ERR_INVALID_STATUS"
            );
        }
    }

    private boolean isTechnicalManagerOfSubmitter(
            User approver,
            User submittedBy
    ) {
        if (approver == null || submittedBy == null) {
            return false;
        }

        if (!approver.isActive()
                || approver.isDeleted()
                || !approver.isManagerFlag()) {
            return false;
        }

        if (!belongsToTechnicalDepartment(approver)
                || !belongsToTechnicalDepartment(submittedBy)) {
            return false;
        }

        User assignedManager =
                submittedBy.getManager();

        if (assignedManager == null) {
            return false;
        }

        return Objects.equals(
                assignedManager.getId(),
                approver.getId()
        );
    }

    private boolean belongsToTechnicalDepartment(
            User user
    ) {
        if (user == null
                || user.getDepartments() == null
                || user.getDepartments().isEmpty()) {
            return false;
        }

        return user.getDepartments()
                .stream()
                .filter(Objects::nonNull)
                .filter(department ->
                        !department.isDeleted()
                )
                .map(Department::getName)
                .filter(StringUtils::hasText)
                .map(this::normalizeName)
                .anyMatch(
                        TECHNICAL_DEPARTMENT_NAMES::contains
                );
    }

    private void validateTechnicalDepartmentUser(
            User user
    ) {
        if (!belongsToTechnicalDepartment(user)) {
            throw new ValidationException(
                    "Only a Technical department user can manage portal details",
                    "ERR_TECHNICAL_DEPARTMENT_REQUIRED"
            );
        }
    }

    private boolean isDepartmentManagerForProject(
            User manager,
            Project project
    ) {
        if (manager == null
                || project == null
                || !manager.isManagerFlag()
                || !manager.isActive()
                || manager.isDeleted()) {
            return false;
        }

        if (manager.getDepartments() == null
                || manager.getDepartments().isEmpty()) {
            return false;
        }

        Set<Long> managerDepartmentIds =
                manager.getDepartments()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(department ->
                                !department.isDeleted()
                        )
                        .map(Department::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        if (managerDepartmentIds.isEmpty()) {
            return false;
        }

        List<ProjectMilestoneAssignment> assignments =
                assignmentRepo
                        .findByProjectIdAndIsDeletedFalse(
                                project.getId()
                        );

        if (assignments == null
                || assignments.isEmpty()) {
            return false;
        }

        return assignments.stream()
                .filter(Objects::nonNull)
                .filter(assignment ->
                        assignment.getAssignedUser() != null
                )
                .anyMatch(assignment -> {
                    User assignedUser =
                            assignment.getAssignedUser();

                    if (assignedUser.getDepartments() == null) {
                        return false;
                    }

                    return assignedUser.getDepartments()
                            .stream()
                            .filter(Objects::nonNull)
                            .filter(department ->
                                    !department.isDeleted()
                            )
                            .map(Department::getId)
                            .filter(Objects::nonNull)
                            .anyMatch(
                                    managerDepartmentIds::contains
                            );
                });
    }

    private Project getProjectAndCheckAccess(
            Long projectId,
            Long userId
    ) {
        if (projectId == null) {
            throw new ValidationException(
                    "Project ID is required",
                    "ERR_PROJECT_ID_REQUIRED"
            );
        }

        Project project =
                projectRepo.findActiveUserById(projectId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Project not found",
                                        "ERR_PROJECT_NOT_FOUND"
                                )
                        );

        User user = getUser(userId);

        if (isAdminOrOperationHead(user)) {
            return project;
        }

        boolean assigned =
                isUserAssignedToProject(
                        projectId,
                        userId
                );

        boolean departmentManager =
                isDepartmentManagerForProject(
                        user,
                        project
                );

        if (!assigned && !departmentManager) {
            throw new ValidationException(
                    "Access denied to this project",
                    "ERR_UNAUTHORIZED_PORTAL_ACCESS"
            );
        }

        return project;
    }

    private ProjectPortalDetail getPortalDetail(
            Long projectId,
            Long detailId
    ) {
        if (detailId == null) {
            throw new ValidationException(
                    "Portal detail ID is required",
                    "ERR_PORTAL_DETAIL_ID_REQUIRED"
            );
        }

        /*
         * Replaces the removed:
         * findByIdAndIsDeletedFalse(detailId)
         */
        return portalDetailRepo
                .findActiveByIdAndProjectId(
                        detailId,
                        projectId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Portal detail not found for this project",
                                "ERR_PORTAL_NOT_FOUND"
                        )
                );
    }

    private User getUser(Long userId) {
        if (userId == null) {
            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        return userRepo.findActiveUserById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Active user not found",
                                "ERR_USER_NOT_FOUND"
                        )
                );
    }

    private boolean isUserAssignedToProject(
            Long projectId,
            Long userId
    ) {
        return assignmentRepo
                .findByProjectIdAndAssignedUserIdAndIsDeletedFalse(
                        projectId,
                        userId
                )
                .isPresent();
    }

    private boolean isAdminOrOperationHead(
            User user
    ) {
        if (user == null
                || user.getRoles() == null
                || user.getRoles().isEmpty()) {
            return false;
        }

        return user.getRoles()
                .stream()
                .filter(Objects::nonNull)
                .filter(role -> !role.isDeleted())
                .map(Role::getName)
                .filter(StringUtils::hasText)
                .map(this::normalizeName)
                .anyMatch(
                        ADMIN_OPERATION_HEAD_ROLES::contains
                );
    }

    private void validateCreateRequest(
            ProjectPortalDetailRequestDto dto
    ) {
        if (dto == null) {
            throw new ValidationException(
                    "Portal request is required",
                    "ERR_PORTAL_REQUEST_REQUIRED"
            );
        }

        if (!StringUtils.hasText(dto.getPortalName())) {
            throw new ValidationException(
                    "Portal name is required",
                    "ERR_PORTAL_NAME_REQUIRED"
            );
        }

        if (!StringUtils.hasText(dto.getUsername())) {
            throw new ValidationException(
                    "Portal username is required",
                    "ERR_PORTAL_USERNAME_REQUIRED"
            );
        }

        if (!StringUtils.hasText(dto.getPassword())) {
            throw new ValidationException(
                    "Portal password is required",
                    "ERR_PORTAL_PASSWORD_REQUIRED"
            );
        }
    }

    private void validateUpdateRequest(
            ProjectPortalDetailRequestDto dto
    ) {
        if (dto == null) {
            throw new ValidationException(
                    "Portal request is required",
                    "ERR_PORTAL_REQUEST_REQUIRED"
            );
        }

        if (!StringUtils.hasText(dto.getPortalName())) {
            throw new ValidationException(
                    "Portal name is required",
                    "ERR_PORTAL_NAME_REQUIRED"
            );
        }

        if (!StringUtils.hasText(dto.getUsername())) {
            throw new ValidationException(
                    "Portal username is required",
                    "ERR_PORTAL_USERNAME_REQUIRED"
            );
        }
    }

    private String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }

    private ProjectPortalDetailResponseDto mapToResponseDto(
            ProjectPortalDetail entity,
            User viewer
    ) {
        ProjectPortalDetailResponseDto dto =
                new ProjectPortalDetailResponseDto();

        dto.setId(entity.getId());

        if (entity.getProject() != null) {
            dto.setProjectId(
                    entity.getProject().getId()
            );
        }

        dto.setPortalName(entity.getPortalName());
        dto.setPortalUrl(entity.getPortalUrl());
        dto.setUsername(entity.getUsername());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedDate(entity.getCreatedDate());

        dto.setCreatedByName(
                entity.getCreatedBy() != null
                        ? entity.getCreatedBy().getFullName()
                        : null
        );

        dto.setUpdatedDate(entity.getUpdatedDate());

        dto.setUpdatedByName(
                entity.getUpdatedBy() != null
                        ? entity.getUpdatedBy().getFullName()
                        : null
        );

        /*
         * ProjectPortalDetailResponseDto currently expects String.
         */
        dto.setStatus(
                entity.getStatus() != null
                        ? entity.getStatus().name()
                        : null
        );

        dto.setApprovedByName(
                entity.getApprovedBy() != null
                        ? entity.getApprovedBy().getFullName()
                        : null
        );

        dto.setApprovalDate(entity.getApprovalDate());
        dto.setApprovalRemarks(
                entity.getApprovalRemarks()
        );

        boolean canViewPassword =
                isAdminOrOperationHead(viewer)
                        || isTechnicalManagerOfSubmitter(
                        viewer,
                        entity.getCreatedBy()
                );

        if (canViewPassword) {
            dto.setPassword(entity.getPassword());
        } else {
            dto.setPassword("********");
        }

        return dto;
    }
}