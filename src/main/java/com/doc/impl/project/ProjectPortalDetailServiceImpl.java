package com.doc.impl.project;

import com.doc.dto.project.portal.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // =========================================================
    // ALLOWED DEPARTMENTS FOR PORTAL MANAGEMENT
    // =========================================================

    private static final Set<String> PORTAL_ALLOWED_DEPARTMENT_NAMES = Set.of(
            "TECHNICAL",
            "TECHNICAL_DEPARTMENT",
            "CRT",
            "CRT_DEPARTMENT"
    );


    // =========================================================
    // ADMIN / OPERATION HEAD OVERRIDE ROLES
    // =========================================================

    private static final Set<String> ADMIN_OPERATION_HEAD_ROLES = Set.of(
            "ADMIN",
            "ROLE_ADMIN",
            "OPERATION_HEAD",
            "ROLE_OPERATION_HEAD"
    );


    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ProjectPortalDetailRepository portalDetailRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final ProjectMilestoneAssignmentRepository assignmentRepo;
    private final PasswordEncoder passwordEncoder;


    // =========================================================
    // CONSTRUCTOR INJECTION
    // =========================================================

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


    // =========================================================
    // 1. ADD PORTAL DETAIL
    // =========================================================

    @Override
    public ProjectPortalDetailResponseDto addPortalDetail(
            Long projectId,
            Long userId,
            ProjectPortalDetailRequestDto dto
    ) {

        log.info(
                "[PORTAL-CREATE-START] projectId={} | userId={}",
                projectId,
                userId
        );

        Project project = getProjectAndCheckAccess(
                projectId,
                userId
        );

        User user = getUser(userId);


        /*
         * ADMIN / OPERATION HEAD:
         * No department restriction.
         *
         * NORMAL USER:
         * Must:
         * 1. Belong to Technical or CRT
         * 2. Be directly assigned to project
         */
        if (!isAdminOrOperationHead(user)) {

            validatePortalDepartmentUser(user);

            boolean assigned =
                    isUserAssignedToProject(
                            projectId,
                            userId
                    );

            log.info(
                    "[PORTAL-CREATE-ACCESS-CHECK] " +
                            "projectId={} | userId={} | assigned={}",
                    projectId,
                    userId,
                    assigned
            );

            if (!assigned) {

                log.warn(
                        "[PORTAL-CREATE-DENIED] " +
                                "projectId={} | userId={} | reason=NOT_ASSIGNED",
                        projectId,
                        userId
                );

                throw new ValidationException(
                        "Only a Technical or CRT department user assigned "
                                + "to this project can add portal details",
                        "ERR_PORTAL_CREATE_NOT_ALLOWED"
                );
            }
        }


        validateCreateRequest(dto);


        String portalName =
                dto.getPortalName().trim();


        // =====================================================
        // DUPLICATE PORTAL VALIDATION
        // =====================================================

        if (portalDetailRepo.existsActivePortalName(
                projectId,
                portalName
        )) {

            throw new ValidationException(
                    "Portal '" + portalName + "' already exists",
                    "ERR_DUPLICATE_PORTAL"
            );
        }


        // =====================================================
        // CREATE ENTITY
        // =====================================================

        ProjectPortalDetail entity =
                new ProjectPortalDetail();

        entity.setProject(project);
        entity.setCompany(project.getCompany());

        entity.setPortalName(portalName);

        entity.setPortalUrl(
                trimToNull(dto.getPortalUrl())
        );

        entity.setUsername(
                dto.getUsername().trim()
        );

        /*
         * Existing behavior preserved.
         *
         * Note:
         * PasswordEncoder normally performs one-way hashing.
         */
        entity.setPassword(
                passwordEncoder.encode(
                        dto.getPassword().trim()
                )
        );

        entity.setRemarks(
                trimToNull(dto.getRemarks())
        );

        entity.setDate(
                LocalDate.now()
        );

        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);
        entity.setDeleted(false);


        // =====================================================
        // APPROVAL STATUS
        // =====================================================

        if (isAdminOrOperationHead(user)) {

            entity.setStatus(
                    ProjectPortalDetailStatus.APPROVED
            );

            entity.setApprovedBy(user);

            entity.setApprovalDate(
                    new Date()
            );

            entity.setApprovalRemarks(
                    "Automatically approved by authorized user"
            );

            log.info(
                    "[PORTAL-CREATE-AUTO-APPROVED] " +
                            "projectId={} | userId={}",
                    projectId,
                    userId
            );

        } else {

            entity.setStatus(
                    ProjectPortalDetailStatus.PENDING
            );

            entity.setApprovedBy(null);
            entity.setApprovalDate(null);
            entity.setApprovalRemarks(null);

            log.info(
                    "[PORTAL-CREATE-PENDING] " +
                            "projectId={} | userId={}",
                    projectId,
                    userId
            );
        }


        ProjectPortalDetail saved =
                portalDetailRepo.save(entity);


        log.info(
                "[PORTAL-CREATE-SUCCESS] " +
                        "projectId={} | detailId={} | createdBy={} | status={}",
                projectId,
                saved.getId(),
                userId,
                saved.getStatus()
        );


        return mapToResponseDto(
                saved,
                user
        );
    }


    // =========================================================
    // 2. GET PORTAL DETAILS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ProjectPortalDetailListResponseDto getPortalDetails(
            Long projectId,
            Long userId
    ) {

        log.info(
                "[PORTAL-GET-START] projectId={} | userId={}",
                projectId,
                userId
        );


        /*
         * This now supports:
         *
         * 1. ADMIN
         * 2. OPERATION HEAD
         * 3. Assigned Technical user
         * 4. Assigned CRT user
         * 5. Technical manager for project
         * 6. CRT manager for project
         */
        Project project =
                getProjectAndCheckAccess(
                        projectId,
                        userId
                );


        User viewer =
                getUser(userId);


        List<ProjectPortalDetail> details =
                portalDetailRepo.findActiveByProjectId(
                        projectId
                );


        List<ProjectPortalDetailResponseDto> portalDtos =
                details.stream()
                        .filter(Objects::nonNull)
                        .map(detail ->
                                mapToResponseDto(
                                        detail,
                                        viewer
                                )
                        )
                        .toList();


        ProjectPortalDetailListResponseDto response =
                new ProjectPortalDetailListResponseDto();


        response.setProjectId(
                project.getId()
        );

        response.setProjectNo(
                project.getProjectNo()
        );


        if (project.getCompany() != null) {

            response.setCompanyName(
                    project.getCompany().getName()
            );
        }


        response.setPortals(
                portalDtos
        );


        log.info(
                "[PORTAL-GET-SUCCESS] " +
                        "projectId={} | userId={} | count={}",
                projectId,
                userId,
                portalDtos.size()
        );


        return response;
    }


    // =========================================================
    // 3. UPDATE PORTAL DETAIL
    // =========================================================

    @Override
    public ProjectPortalDetailResponseDto updatePortalDetail(
            Long projectId,
            Long detailId,
            Long userId,
            ProjectPortalDetailRequestDto dto
    ) {

        log.info(
                "[PORTAL-UPDATE-START] " +
                        "projectId={} | detailId={} | userId={}",
                projectId,
                detailId,
                userId
        );


        getProjectAndCheckAccess(
                projectId,
                userId
        );


        ProjectPortalDetail entity =
                getPortalDetail(
                        projectId,
                        detailId
                );


        User user =
                getUser(userId);


        if (!isAdminOrOperationHead(user)) {

            validatePortalDepartmentUser(user);

            boolean assigned =
                    isUserAssignedToProject(
                            projectId,
                            userId
                    );


            if (!assigned) {

                log.warn(
                        "[PORTAL-UPDATE-DENIED] " +
                                "projectId={} | detailId={} | userId={} | reason=NOT_ASSIGNED",
                        projectId,
                        detailId,
                        userId
                );

                throw new ValidationException(
                        "Only a Technical or CRT department user assigned "
                                + "to this project can update portal details",
                        "ERR_PORTAL_UPDATE_NOT_ALLOWED"
                );
            }
        }


        validateUpdateRequest(dto);


        String portalName =
                dto.getPortalName().trim();


        // =====================================================
        // DUPLICATE VALIDATION
        // =====================================================

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


        // =====================================================
        // UPDATE VALUES
        // =====================================================

        entity.setPortalName(
                portalName
        );

        entity.setPortalUrl(
                trimToNull(
                        dto.getPortalUrl()
                )
        );

        entity.setUsername(
                dto.getUsername().trim()
        );

        entity.setRemarks(
                trimToNull(
                        dto.getRemarks()
                )
        );

        entity.setUpdatedBy(
                user
        );


        /*
         * Password is optional during update.
         * Only update when supplied.
         */
        if (StringUtils.hasText(
                dto.getPassword()
        )) {

            entity.setPassword(
                    passwordEncoder.encode(
                            dto.getPassword().trim()
                    )
            );
        }


        // =====================================================
        // RE-APPROVAL REQUIRED FOR NORMAL USERS
        // =====================================================

        if (!isAdminOrOperationHead(user)) {

            entity.setStatus(
                    ProjectPortalDetailStatus.PENDING
            );

            entity.setApprovedBy(null);
            entity.setApprovalDate(null);
            entity.setApprovalRemarks(null);


            log.info(
                    "[PORTAL-UPDATE-REAPPROVAL-REQUIRED] " +
                            "projectId={} | detailId={} | userId={}",
                    projectId,
                    detailId,
                    userId
            );
        }


        ProjectPortalDetail saved =
                portalDetailRepo.save(entity);


        log.info(
                "[PORTAL-UPDATE-SUCCESS] " +
                        "projectId={} | detailId={} | updatedBy={} | status={}",
                projectId,
                detailId,
                userId,
                saved.getStatus()
        );


        return mapToResponseDto(
                saved,
                user
        );
    }


    // =========================================================
    // 4. DELETE PORTAL DETAIL
    // =========================================================

    @Override
    public void deletePortalDetail(
            Long projectId,
            Long detailId,
            Long userId
    ) {

        log.info(
                "[PORTAL-DELETE-START] " +
                        "projectId={} | detailId={} | userId={}",
                projectId,
                detailId,
                userId
        );


        getProjectAndCheckAccess(
                projectId,
                userId
        );


        ProjectPortalDetail entity =
                getPortalDetail(
                        projectId,
                        detailId
                );


        User user =
                getUser(userId);


        if (!isAdminOrOperationHead(user)) {

            validatePortalDepartmentUser(user);


            boolean assigned =
                    isUserAssignedToProject(
                            projectId,
                            userId
                    );


            if (!assigned) {

                log.warn(
                        "[PORTAL-DELETE-DENIED] " +
                                "projectId={} | detailId={} | userId={} | reason=NOT_ASSIGNED",
                        projectId,
                        detailId,
                        userId
                );

                throw new ValidationException(
                        "Only a Technical or CRT department user assigned "
                                + "to this project can delete portal details",
                        "ERR_PORTAL_DELETE_NOT_ALLOWED"
                );
            }
        }


        entity.setDeleted(true);

        entity.setUpdatedBy(
                user
        );


        portalDetailRepo.save(entity);


        log.info(
                "[PORTAL-DELETE-SUCCESS] " +
                        "projectId={} | detailId={} | deletedBy={}",
                projectId,
                detailId,
                userId
        );
    }


    // =========================================================
    // 5. APPROVE / REJECT
    // =========================================================

    @Override
    public ProjectPortalDetailResponseDto approveOrRejectPortalDetail(
            Long projectId,
            Long detailId,
            Long userId,
            ProjectPortalDetailApprovalDto approvalDto
    ) {

        log.info(
                "[PORTAL-APPROVAL-START] " +
                        "projectId={} | detailId={} | approverId={}",
                projectId,
                detailId,
                userId
        );


        /*
         * Admin / Operation Head or relevant department manager
         * should have project access.
         */
        getProjectAndCheckAccess(
                projectId,
                userId
        );


        ProjectPortalDetail entity =
                getPortalDetail(
                        projectId,
                        detailId
                );


        if (entity.getStatus()
                != ProjectPortalDetailStatus.PENDING) {

            throw new ValidationException(
                    "Only PENDING portal details can be approved or rejected",
                    "ERR_NOT_PENDING"
            );
        }


        User approver =
                getUser(userId);


        boolean canApprove =
                isAdminOrOperationHead(approver)
                        || isPortalDepartmentManagerOfSubmitter(
                        approver,
                        entity.getCreatedBy()
                );


        if (!canApprove) {

            log.warn(
                    "[PORTAL-APPROVAL-DENIED] " +
                            "projectId={} | detailId={} | approverId={} | submittedBy={}",
                    projectId,
                    detailId,
                    userId,
                    entity.getCreatedBy() != null
                            ? entity.getCreatedBy().getId()
                            : null
            );


            throw new ValidationException(
                    "Only the submitter's Technical/CRT department manager, "
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


        ProjectPortalDetailStatus action =
                parseApprovalStatus(
                        approvalDto.getStatus()
                );


        String approvalRemarks =
                trimToNull(
                        approvalDto.getApprovalRemarks()
                );


        if (action == ProjectPortalDetailStatus.REJECTED
                && !StringUtils.hasText(
                approvalRemarks
        )) {

            throw new ValidationException(
                    "Approval remarks are required when rejecting portal details",
                    "ERR_REJECTION_REMARKS_REQUIRED"
            );
        }


        entity.setStatus(
                action
        );

        entity.setApprovedBy(
                approver
        );

        entity.setApprovalDate(
                new Date()
        );

        entity.setApprovalRemarks(
                approvalRemarks
        );

        entity.setUpdatedBy(
                approver
        );


        ProjectPortalDetail saved =
                portalDetailRepo.save(entity);


        log.info(
                "[PORTAL-APPROVAL-SUCCESS] " +
                        "projectId={} | detailId={} | approverId={} | status={}",
                projectId,
                detailId,
                userId,
                saved.getStatus()
        );


        return mapToResponseDto(
                saved,
                approver
        );
    }


    // =========================================================
    // 6. APPROVAL QUEUE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ProjectPortalApprovalQueueResponseDto getApprovalQueue(
            Long userId,
            ProjectPortalDetailStatus status,
            int page,
            int size
    ) {

        ProjectPortalDetailStatus requestedStatus =
                status != null
                        ? status
                        : ProjectPortalDetailStatus.PENDING;


        log.info(
                "[PORTAL-QUEUE-START] " +
                        "userId={} | status={} | page={} | size={}",
                userId,
                requestedStatus,
                page,
                size
        );


        User loggedInUser =
                getUser(userId);


        boolean adminOrOperationHead =
                isAdminOrOperationHead(
                        loggedInUser
                );


        boolean portalDepartmentManager =
                loggedInUser.isManagerFlag()
                        && belongsToPortalDepartment(
                        loggedInUser
                );


        if (!adminOrOperationHead
                && !portalDepartmentManager) {

            log.warn(
                    "[PORTAL-QUEUE-DENIED] userId={}",
                    userId
            );


            throw new ValidationException(
                    "Only a Technical/CRT department manager, Admin, "
                            + "or Operation Head can access the portal approval queue",
                    "ERR_PORTAL_APPROVAL_QUEUE_UNAUTHORIZED"
            );
        }


        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdDate"),
                                Sort.Order.desc("id")
                        )
                );


        Page<ProjectPortalDetail> portalPage;


        if (adminOrOperationHead) {

            portalPage =
                    portalDetailRepo.findAllActiveByStatus(
                            requestedStatus,
                            pageable
                    );

        } else {

            /*
             * Existing repository method can remain if its query only
             * uses manager ID.
             *
             * Rename later if desired because it now supports CRT too.
             */
            portalPage =
                    portalDetailRepo
                            .findTechnicalPortalRequestsForManager(
                                    userId,
                                    requestedStatus,
                                    pageable
                            );
        }


        List<ProjectPortalDetailResponseDto> requestDtos =
                portalPage.getContent()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(portal ->
                                mapToResponseDto(
                                        portal,
                                        loggedInUser
                                )
                        )
                        .toList();


        ProjectPortalApprovalQueueResponseDto response =
                new ProjectPortalApprovalQueueResponseDto();


        response.setUserId(
                userId
        );

        response.setRequestedStatus(
                requestedStatus.name()
        );

        response.setTotalRequests(
                portalPage.getTotalElements()
        );

        response.setTotalPages(
                portalPage.getTotalPages()
        );

        response.setCurrentPage(
                portalPage.getNumber() + 1
        );

        response.setPageSize(
                portalPage.getSize()
        );

        response.setFirst(
                portalPage.isFirst()
        );

        response.setLast(
                portalPage.isLast()
        );

        response.setHasNext(
                portalPage.hasNext()
        );

        response.setHasPrevious(
                portalPage.hasPrevious()
        );

        response.setRequests(
                requestDtos
        );


        log.info(
                "[PORTAL-QUEUE-SUCCESS] " +
                        "userId={} | status={} | currentPage={} | " +
                        "pageSize={} | records={} | totalRequests={} | accessType={}",
                userId,
                requestedStatus,
                response.getCurrentPage(),
                response.getPageSize(),
                requestDtos.size(),
                portalPage.getTotalElements(),
                adminOrOperationHead
                        ? "ADMIN_OR_OPERATION_HEAD"
                        : "PORTAL_DEPARTMENT_MANAGER"
        );


        return response;
    }


    // =========================================================
    // APPROVAL STATUS PARSER
    // =========================================================

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


    // =========================================================
    // PORTAL DEPARTMENT MANAGER CHECK
    // =========================================================

    private boolean isPortalDepartmentManagerOfSubmitter(
            User approver,
            User submittedBy
    ) {

        if (approver == null
                || submittedBy == null) {

            return false;
        }


        if (!approver.isActive()
                || approver.isDeleted()
                || !approver.isManagerFlag()) {

            return false;
        }


        /*
         * Both users must belong to Technical / CRT portal
         * enabled department.
         */
        if (!belongsToPortalDepartment(approver)
                || !belongsToPortalDepartment(submittedBy)) {

            return false;
        }


        User assignedManager =
                submittedBy.getManager();


        if (assignedManager == null) {

            return false;
        }


        if (!Objects.equals(
                assignedManager.getId(),
                approver.getId()
        )) {

            return false;
        }


        /*
         * Manager and submitter must share an allowed portal
         * department.
         */
        Set<Long> managerDepartmentIds =
                approver.getDepartments()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(this::isPortalDepartment)
                        .map(Department::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());


        if (managerDepartmentIds.isEmpty()) {

            return false;
        }


        boolean samePortalDepartment =
                submittedBy.getDepartments()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(this::isPortalDepartment)
                        .map(Department::getId)
                        .filter(Objects::nonNull)
                        .anyMatch(
                                managerDepartmentIds::contains
                        );


        log.info(
                "[PORTAL-MANAGER-CHECK] " +
                        "approverId={} | submitterId={} | samePortalDepartment={}",
                approver.getId(),
                submittedBy.getId(),
                samePortalDepartment
        );


        return samePortalDepartment;
    }


    // =========================================================
    // USER BELONGS TO TECHNICAL / CRT
    // =========================================================

    private boolean belongsToPortalDepartment(
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
                .map(
                        Department::getName
                )
                .filter(
                        StringUtils::hasText
                )
                .map(
                        this::normalizeName
                )
                .anyMatch(
                        PORTAL_ALLOWED_DEPARTMENT_NAMES::contains
                );
    }


    // =========================================================
    // CHECK INDIVIDUAL DEPARTMENT
    // =========================================================

    private boolean isPortalDepartment(
            Department department
    ) {

        if (department == null
                || department.isDeleted()
                || !StringUtils.hasText(
                department.getName()
        )) {

            return false;
        }


        return PORTAL_ALLOWED_DEPARTMENT_NAMES.contains(
                normalizeName(
                        department.getName()
                )
        );
    }


    // =========================================================
    // VALIDATE TECHNICAL / CRT USER
    // =========================================================

    private void validatePortalDepartmentUser(
            User user
    ) {

        if (!belongsToPortalDepartment(user)) {

            log.warn(
                    "[PORTAL-DEPARTMENT-VALIDATION-FAILED] userId={}",
                    user != null
                            ? user.getId()
                            : null
            );


            throw new ValidationException(
                    "Only Technical or CRT department users "
                            + "can manage portal details",
                    "ERR_PORTAL_DEPARTMENT_REQUIRED"
            );
        }
    }


    // =========================================================
    // DEPARTMENT MANAGER PROJECT ACCESS
    // =========================================================

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


        /*
         * Only Technical / CRT departments are relevant.
         */
        Set<Long> managerDepartmentIds =
                manager.getDepartments()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(this::isPortalDepartment)
                        .map(
                                Department::getId
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .collect(
                                Collectors.toSet()
                        );


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


        boolean managerAccess =
                assignments.stream()
                        .filter(Objects::nonNull)
                        .filter(assignment ->
                                assignment.getAssignedUser() != null
                        )
                        .anyMatch(assignment -> {

                            User assignedUser =
                                    assignment.getAssignedUser();


                            if (assignedUser.getDepartments() == null
                                    || assignedUser
                                    .getDepartments()
                                    .isEmpty()) {

                                return false;
                            }


                            return assignedUser.getDepartments()
                                    .stream()
                                    .filter(Objects::nonNull)
                                    .filter(this::isPortalDepartment)
                                    .map(
                                            Department::getId
                                    )
                                    .filter(
                                            Objects::nonNull
                                    )
                                    .anyMatch(
                                            managerDepartmentIds::contains
                                    );
                        });


        log.info(
                "[PORTAL-DEPARTMENT-MANAGER-CHECK] " +
                        "projectId={} | managerId={} | access={}",
                project.getId(),
                manager.getId(),
                managerAccess
        );


        return managerAccess;
    }


    // =========================================================
    // PROJECT ACCESS VALIDATION
    // =========================================================

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


        if (userId == null) {

            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }


        Project project =
                projectRepo.findActiveUserById(
                                projectId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Project not found",
                                        "ERR_PROJECT_NOT_FOUND"
                                )
                        );


        User user =
                getUser(userId);


        log.info(
                "[PORTAL-ACCESS-CHECK-START] " +
                        "projectId={} | userId={}",
                projectId,
                userId
        );


        // =====================================================
        // ADMIN / OPERATION HEAD
        // =====================================================

        if (isAdminOrOperationHead(user)) {

            log.info(
                    "[PORTAL-ACCESS-GRANTED] " +
                            "projectId={} | userId={} | reason=ADMIN_OR_OPERATION_HEAD",
                    projectId,
                    userId
            );

            return project;
        }


        /*
         * For portal features a normal user / manager must
         * belong to Technical or CRT.
         */
        validatePortalDepartmentUser(user);


        // =====================================================
        // DIRECT PROJECT ASSIGNMENT
        // =====================================================

        boolean directlyAssigned =
                isUserAssignedToProject(
                        projectId,
                        userId
                );


        // =====================================================
        // DEPARTMENT MANAGER ACCESS
        // =====================================================

        boolean departmentManager =
                isDepartmentManagerForProject(
                        user,
                        project
                );


        log.info(
                "[PORTAL-ACCESS-CHECK] " +
                        "projectId={} | userId={} | directlyAssigned={} | departmentManager={}",
                projectId,
                userId,
                directlyAssigned,
                departmentManager
        );


        if (!directlyAssigned
                && !departmentManager) {

            log.warn(
                    "[PORTAL-ACCESS-DENIED] " +
                            "projectId={} | userId={} | directlyAssigned={} | departmentManager={}",
                    projectId,
                    userId,
                    directlyAssigned,
                    departmentManager
            );


            throw new ValidationException(
                    "Access denied. User must be an assigned Technical/CRT "
                            + "user or the relevant department manager",
                    "ERR_UNAUTHORIZED_PORTAL_ACCESS"
            );
        }


        log.info(
                "[PORTAL-ACCESS-GRANTED] " +
                        "projectId={} | userId={} | reason={}",
                projectId,
                userId,
                directlyAssigned
                        ? "DIRECT_PROJECT_ASSIGNMENT"
                        : "DEPARTMENT_MANAGER"
        );


        return project;
    }


    // =========================================================
    // GET SINGLE PORTAL DETAIL
    // =========================================================

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


    // =========================================================
    // GET ACTIVE USER
    // =========================================================

    private User getUser(
            Long userId
    ) {

        if (userId == null) {

            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }


        return userRepo
                .findActiveUserById(
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Active user not found",
                                "ERR_USER_NOT_FOUND"
                        )
                );
    }


    // =========================================================
    // DIRECT PROJECT ASSIGNMENT CHECK
    // =========================================================

    private boolean isUserAssignedToProject(
            Long projectId,
            Long userId
    ) {

        boolean assigned =
                assignmentRepo
                        .existsActiveAssignmentForUser(
                                projectId,
                                userId
                        );


        log.info(
                "[PORTAL-PROJECT-ASSIGNMENT-CHECK] " +
                        "projectId={} | userId={} | assigned={}",
                projectId,
                userId,
                assigned
        );


        return assigned;
    }


    // =========================================================
    // ADMIN / OPERATION HEAD CHECK
    // =========================================================

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
                .filter(
                        Objects::nonNull
                )
                .filter(
                        role ->
                                !role.isDeleted()
                )
                .map(
                        Role::getName
                )
                .filter(
                        StringUtils::hasText
                )
                .map(
                        this::normalizeName
                )
                .anyMatch(
                        ADMIN_OPERATION_HEAD_ROLES::contains
                );
    }


    // =========================================================
    // CREATE REQUEST VALIDATION
    // =========================================================

    private void validateCreateRequest(
            ProjectPortalDetailRequestDto dto
    ) {

        if (dto == null) {

            throw new ValidationException(
                    "Portal request is required",
                    "ERR_PORTAL_REQUEST_REQUIRED"
            );
        }


        if (!StringUtils.hasText(
                dto.getPortalName()
        )) {

            throw new ValidationException(
                    "Portal name is required",
                    "ERR_PORTAL_NAME_REQUIRED"
            );
        }


        if (!StringUtils.hasText(
                dto.getUsername()
        )) {

            throw new ValidationException(
                    "Portal username is required",
                    "ERR_PORTAL_USERNAME_REQUIRED"
            );
        }


        if (!StringUtils.hasText(
                dto.getPassword()
        )) {

            throw new ValidationException(
                    "Portal password is required",
                    "ERR_PORTAL_PASSWORD_REQUIRED"
            );
        }
    }


    // =========================================================
    // UPDATE REQUEST VALIDATION
    // =========================================================

    private void validateUpdateRequest(
            ProjectPortalDetailRequestDto dto
    ) {

        if (dto == null) {

            throw new ValidationException(
                    "Portal request is required",
                    "ERR_PORTAL_REQUEST_REQUIRED"
            );
        }


        if (!StringUtils.hasText(
                dto.getPortalName()
        )) {

            throw new ValidationException(
                    "Portal name is required",
                    "ERR_PORTAL_NAME_REQUIRED"
            );
        }


        if (!StringUtils.hasText(
                dto.getUsername()
        )) {

            throw new ValidationException(
                    "Portal username is required",
                    "ERR_PORTAL_USERNAME_REQUIRED"
            );
        }
    }


    // =========================================================
    // NORMALIZE NAME
    // =========================================================

    private String normalizeName(
            String value
    ) {

        if (!StringUtils.hasText(value)) {

            return "";
        }


        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll(
                        "[^A-Z0-9]+",
                        "_"
                )
                .replaceAll(
                        "^_+|_+$",
                        ""
                );
    }


    // =========================================================
    // TRIM TO NULL
    // =========================================================

    private String trimToNull(
            String value
    ) {

        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }


    // =========================================================
    // MAP ENTITY TO RESPONSE DTO
    // =========================================================

    private ProjectPortalDetailResponseDto mapToResponseDto(
            ProjectPortalDetail entity,
            User viewer
    ) {

        ProjectPortalDetailResponseDto dto =
                new ProjectPortalDetailResponseDto();


        dto.setId(
                entity.getId()
        );


        if (entity.getProject() != null) {

            dto.setProjectId(
                    entity.getProject().getId()
            );
        }


        dto.setPortalName(
                entity.getPortalName()
        );

        dto.setPortalUrl(
                entity.getPortalUrl()
        );

        dto.setUsername(
                entity.getUsername()
        );

        dto.setRemarks(
                entity.getRemarks()
        );

        dto.setCreatedDate(
                entity.getCreatedDate()
        );


        dto.setCreatedByName(
                entity.getCreatedBy() != null
                        ? entity.getCreatedBy().getFullName()
                        : null
        );


        dto.setUpdatedDate(
                entity.getUpdatedDate()
        );


        dto.setUpdatedByName(
                entity.getUpdatedBy() != null
                        ? entity.getUpdatedBy().getFullName()
                        : null
        );


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


        dto.setApprovalDate(
                entity.getApprovalDate()
        );


        dto.setApprovalRemarks(
                entity.getApprovalRemarks()
        );


        /*
         * Password visibility:
         *
         * ADMIN / OPERATION HEAD
         * OR
         * Direct manager of the Technical/CRT submitter
         */
        boolean canViewPassword =
                isAdminOrOperationHead(viewer)
                        || isPortalDepartmentManagerOfSubmitter(
                        viewer,
                        entity.getCreatedBy()
                );


        if (canViewPassword) {

            dto.setPassword(
                    entity.getPassword()
            );

        } else {

            dto.setPassword(
                    "********"
            );
        }


        return dto;
    }
}