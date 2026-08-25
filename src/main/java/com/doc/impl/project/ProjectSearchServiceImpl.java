package com.doc.impl.project;

import com.doc.dto.project.ProjectCountResponseDto;
import com.doc.dto.project.ProjectResponseDto;
import com.doc.entity.department.Department;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.UserRepository;
import com.doc.service.ProjectSearchService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectSearchServiceImpl implements ProjectSearchService {

    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Override
    public List<ProjectResponseDto> searchProjectsByCompanyName(
            String companyName,
            Long userId
    ) {
        return searchProjects(
                "company",
                companyName,
                userId,
                null,
                null,
                null
        );
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByProjectNumber(
            String projectNumber,
            Long userId
    ) {
        return searchProjects(
                "project-number",
                projectNumber,
                userId,
                null,
                null,
                null
        );
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByContactName(
            String contactName,
            Long userId
    ) {
        return searchProjects(
                "contact",
                contactName,
                userId,
                null,
                null,
                null
        );
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByProjectName(
            String projectName,
            Long userId
    ) {
        return searchProjects(
                "project-name",
                projectName,
                userId,
                null,
                null,
                null
        );
    }

    @Override
    public List<ProjectResponseDto> searchProjects(
            String type,
            String value,
            Long userId,
            String statusName,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        log.info(
                "[PROJECT-SEARCH-START] type={} | value={} | userId={} | " +
                        "status={} | fromDate={} | toDate={}",
                type,
                value,
                userId,
                statusName,
                fromDate,
                toDate
        );

        validateSearchRequest(
                type,
                userId,
                fromDate,
                toDate
        );

        User requestingUser = findActiveUser(userId);

        boolean isAdmin = hasRole(requestingUser, "ADMIN");

        boolean isOperationHead =
                hasRole(requestingUser, "OPERATION_HEAD");

        boolean fullAccess = isAdmin || isOperationHead;

        boolean departmentManager =
                requestingUser.isManagerFlag();

        List<Long> departmentIds =
                getDepartmentIds(requestingUser);

        log.info(
                "[PROJECT-SEARCH-ACCESS] userId={} | admin={} | " +
                        "operationHead={} | manager={} | departmentIds={}",
                userId,
                isAdmin,
                isOperationHead,
                departmentManager,
                departmentIds
        );

        CriteriaBuilder criteriaBuilder =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Project> criteriaQuery =
                criteriaBuilder.createQuery(Project.class);

        Root<Project> projectRoot =
                criteriaQuery.from(Project.class);

        /*
         * Fetch required relations to prevent N+1 queries while mapping.
         */
        projectRoot.fetch("status", JoinType.LEFT);
        projectRoot.fetch("product", JoinType.LEFT);
        projectRoot.fetch("company", JoinType.LEFT);
        projectRoot.fetch("contact", JoinType.LEFT);

        Fetch<Project, Object> paymentFetch =
                projectRoot.fetch("paymentDetail", JoinType.LEFT);

        paymentFetch.fetch("paymentType", JoinType.LEFT);
        paymentFetch.fetch("approvedBy", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(
                criteriaBuilder.isFalse(
                        projectRoot.get("isDeleted")
                )
        );

        /*
         * Preserve existing search behaviour:
         * cancelled projects are not returned.
         */
        predicates.add(
                criteriaBuilder.isFalse(
                        projectRoot.get("isCancelled")
                )
        );

        addSearchPredicate(
                type,
                value,
                criteriaBuilder,
                projectRoot,
                predicates
        );

        addStatusPredicate(
                statusName,
                criteriaBuilder,
                projectRoot,
                predicates
        );

        addDatePredicates(
                fromDate,
                toDate,
                criteriaBuilder,
                projectRoot,
                predicates
        );

        /*
         * Apply access control directly inside the database query.
         */
        if (!fullAccess) {
            predicates.add(
                    buildProjectAccessPredicate(
                            requestingUser,
                            departmentManager,
                            departmentIds,
                            criteriaBuilder,
                            criteriaQuery,
                            projectRoot
                    )
            );
        }

        criteriaQuery
                .select(projectRoot)
                .where(
                        criteriaBuilder.and(
                                predicates.toArray(new Predicate[0])
                        )
                )
                .orderBy(
                        criteriaBuilder.desc(
                                projectRoot.get("createdDate")
                        )
                )
                .distinct(true);

        List<Project> projects =
                entityManager.createQuery(criteriaQuery)
                        .getResultList();

        log.info(
                "[PROJECT-SEARCH-SUCCESS] userId={} | type={} | " +
                        "value={} | resultCount={}",
                userId,
                type,
                value,
                projects.size()
        );

        return projects.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Access rules:
     *
     * Executive:
     * A visible milestone must be assigned to the requesting user.
     *
     * Manager:
     * A visible milestone must be mapped to one of the manager's
     * departments. assignedUser may be null.
     *
     * ADMIN/OPERATION_HEAD:
     * This predicate is not added.
     */
    private Predicate buildProjectAccessPredicate(
            User requestingUser,
            boolean departmentManager,
            List<Long> departmentIds,
            CriteriaBuilder criteriaBuilder,
            CriteriaQuery<?> criteriaQuery,
            Root<Project> projectRoot
    ) {
        Subquery<Long> accessSubquery =
                criteriaQuery.subquery(Long.class);

        Root<ProjectMilestoneAssignment> assignment =
                accessSubquery.from(
                        ProjectMilestoneAssignment.class
                );

        Join<ProjectMilestoneAssignment, User> assignedUser =
                assignment.join(
                        "assignedUser",
                        JoinType.LEFT
                );

        Predicate sameProject = criteriaBuilder.equal(
                assignment.get("project"),
                projectRoot
        );

        Predicate activeAssignment = criteriaBuilder.isFalse(
                assignment.get("isDeleted")
        );

        Predicate visibleAssignment = criteriaBuilder.isTrue(
                assignment.get("isVisible")
        );

        /*
         * Executive access:
         * Only their own visible assigned milestone.
         */
        Predicate executiveAccess = criteriaBuilder.and(
                visibleAssignment,
                criteriaBuilder.equal(
                        assignedUser.get("id"),
                        requestingUser.getId()
                )
        );

        Predicate finalAccessPredicate = executiveAccess;

        /*
         * Department manager access:
         * Visible milestone mapped to the manager's department.
         * It does not require assignedUser.
         */
        if (departmentManager && !departmentIds.isEmpty()) {
            Join<ProjectMilestoneAssignment, Object> milestone =
                    assignment.join(
                            "milestone",
                            JoinType.LEFT
                    );

            Join<Object, Object> milestoneDepartment =
                    milestone.join(
                            "departments",
                            JoinType.LEFT
                    );

            Predicate departmentAccess =
                    milestoneDepartment
                            .get("id")
                            .in(departmentIds);

            finalAccessPredicate = criteriaBuilder.or(
                    executiveAccess,
                    departmentAccess
            );
        }

        accessSubquery
                .select(assignment.get("id"))
                .where(
                        criteriaBuilder.and(
                                sameProject,
                                activeAssignment,
                                visibleAssignment,
                                finalAccessPredicate
                        )
                );

        return criteriaBuilder.exists(accessSubquery);
    }

    private void addSearchPredicate(
            String type,
            String value,
            CriteriaBuilder criteriaBuilder,
            Root<Project> projectRoot,
            List<Predicate> predicates
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        String normalizedType =
                normalizeSearchType(type);

        String likeValue =
                "%" + value.trim().toLowerCase(Locale.ROOT) + "%";

        switch (normalizedType) {
            case "company" -> {
                Join<Project, Object> companyJoin =
                        projectRoot.join(
                                "company",
                                JoinType.LEFT
                        );

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        companyJoin.get("name")
                                ),
                                likeValue
                        )
                );
            }

            case "project-number" ->
                    predicates.add(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            projectRoot.get("projectNo")
                                    ),
                                    likeValue
                            )
                    );

            case "contact" -> {
                Join<Project, Object> contactJoin =
                        projectRoot.join(
                                "contact",
                                JoinType.LEFT
                        );

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        contactJoin.get("name")
                                ),
                                likeValue
                        )
                );
            }

            case "project-name" ->
                    predicates.add(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            projectRoot.get("name")
                                    ),
                                    likeValue
                            )
                    );

            default -> throw new IllegalArgumentException(
                    "Invalid search type: " + type
            );
        }
    }

    private void addStatusPredicate(
            String statusName,
            CriteriaBuilder criteriaBuilder,
            Root<Project> projectRoot,
            List<Predicate> predicates
    ) {
        if (statusName == null
                || statusName.isBlank()
                || "ALL".equalsIgnoreCase(statusName)) {
            return;
        }

        predicates.add(
                criteriaBuilder.equal(
                        criteriaBuilder.upper(
                                projectRoot.get("status").get("name")
                        ),
                        statusName.trim().toUpperCase(Locale.ROOT)
                )
        );
    }

    private void addDatePredicates(
            LocalDate fromDate,
            LocalDate toDate,
            CriteriaBuilder criteriaBuilder,
            Root<Project> projectRoot,
            List<Predicate> predicates
    ) {
        if (fromDate != null && toDate != null) {
            predicates.add(
                    criteriaBuilder.between(
                            projectRoot.get("date"),
                            fromDate,
                            toDate
                    )
            );

            return;
        }

        if (fromDate != null) {
            predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(
                            projectRoot.get("date"),
                            fromDate
                    )
            );
        }

        if (toDate != null) {
            predicates.add(
                    criteriaBuilder.lessThanOrEqualTo(
                            projectRoot.get("date"),
                            toDate
                    )
            );
        }
    }

    private String normalizeSearchType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Search type is required"
            );
        }

        String normalized =
                type.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "company", "company-name", "company_name" ->
                    "company";

            case "project-number",
                    "projectnumber",
                    "project_number" ->
                    "project-number";

            case "contact",
                    "contact-name",
                    "contact_name" ->
                    "contact";

            case "project-name",
                    "projectname",
                    "project_name" ->
                    "project-name";

            default -> throw new IllegalArgumentException(
                    "Invalid search type: " + type
            );
        };
    }

    private void validateSearchRequest(
            String type,
            Long userId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID is required"
            );
        }

        normalizeSearchType(type);

        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {

            throw new IllegalArgumentException(
                    "From date cannot be after to date"
            );
        }
    }

    private User findActiveUser(Long userId) {
        return userRepository.findActiveUserById(userId)
                .orElseThrow(() -> {
                    log.warn(
                            "[PROJECT-SEARCH-USER-NOT-FOUND] userId={}",
                            userId
                    );

                    return new ResourceNotFoundException(
                            "User not found or deleted with ID: " + userId,
                            "ERR_USER_NOT_FOUND"
                    );
                });
    }

    private List<Long> getDepartmentIds(User user) {
        if (!user.isManagerFlag()
                || user.getDepartments() == null) {
            return List.of();
        }

        return user.getDepartments()
                .stream()
                .filter(Objects::nonNull)
                .map(Department::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private boolean hasRole(
            User user,
            String requiredRole
    ) {
        if (user.getRoles() == null) {
            return false;
        }

        return user.getRoles()
                .stream()
                .filter(Objects::nonNull)
                .map(role -> role.getName())
                .filter(Objects::nonNull)
                .anyMatch(roleName ->
                        requiredRole.equalsIgnoreCase(
                                roleName.trim()
                        )
                );
    }

    private ProjectResponseDto mapToResponseDto(
            Project project
    ) {
        ProjectResponseDto dto = new ProjectResponseDto();

        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setProjectNo(project.getProjectNo());
        dto.setUnbilledNumber(project.getUnbilledNumber());
        dto.setEstimateNumber(project.getEstimateNumber());

        dto.setSalesPersonId(project.getSalesPersonId());
        dto.setSalesPersonName(project.getSalesPersonName());

        if (project.getProduct() != null) {
            dto.setProductId(project.getProduct().getId());
        }

        if (project.getCompany() != null) {
            dto.setCompanyId(project.getCompany().getId());
            dto.setCompanyName(project.getCompany().getName());
        }

        if (project.getContact() != null) {
            dto.setContactId(project.getContact().getId());
            dto.setContactName(project.getContact().getName());
        }

        dto.setLeadId(project.getLeadId());
        dto.setDate(project.getDate());

        if (project.getPaymentDetail() != null) {
            dto.setTotalAmount(
                    project.getPaymentDetail().getTotalAmount()
            );

            dto.setDueAmount(
                    project.getPaymentDetail().getDueAmount()
            );

            if (project.getPaymentDetail().getPaymentType() != null) {
                dto.setPaymentTypeId(
                        project.getPaymentDetail()
                                .getPaymentType()
                                .getId()
                );
            }

            if (project.getPaymentDetail().getApprovedBy() != null) {
                dto.setApprovedById(
                        project.getPaymentDetail()
                                .getApprovedBy()
                                .getId()
                );
            }
        } else {
            dto.setTotalAmount(0.0);
            dto.setDueAmount(0.0);
        }

        if (project.getStatus() != null) {
            dto.setStatusId(project.getStatus().getId());
            dto.setStatusName(project.getStatus().getName());
        }

        dto.setCreatedDate(project.getCreatedDate());
        dto.setUpdatedDate(project.getUpdatedDate());
        dto.setDeleted(project.isDeleted());
        dto.setActive(project.isActive());

        return dto;
    }

    @Override
    public List<ProjectCountResponseDto> countOfProject(
            Long userId
    ) {
        log.info(
                "[PROJECT-COUNT-START] userId={}",
                userId
        );

        User requestingUser = findActiveUser(userId);

        long total = countAccessibleProjects(
                requestingUser,
                null
        );

        long open = countAccessibleProjects(
                requestingUser,
                "OPEN"
        );

        long inProgress = countAccessibleProjects(
                requestingUser,
                "IN_PROGRESS"
        );

        long completed = countAccessibleProjects(
                requestingUser,
                "COMPLETED"
        );

        ProjectCountResponseDto response =
                new ProjectCountResponseDto();

        response.setTotalProject(String.valueOf(total));
        response.setOpenProject(String.valueOf(open));
        response.setInProgressProject(
                String.valueOf(inProgress)
        );
        response.setCompletedProject(
                String.valueOf(completed)
        );

        log.info(
                "[PROJECT-COUNT-SUCCESS] userId={} | total={} | " +
                        "open={} | inProgress={} | completed={}",
                userId,
                total,
                open,
                inProgress,
                completed
        );

        return List.of(response);
    }

    private long countAccessibleProjects(
            User requestingUser,
            String statusName
    ) {
        boolean fullAccess =
                hasRole(requestingUser, "ADMIN")
                        || hasRole(
                        requestingUser,
                        "OPERATION_HEAD"
                );

        boolean departmentManager =
                requestingUser.isManagerFlag();

        List<Long> departmentIds =
                getDepartmentIds(requestingUser);

        CriteriaBuilder criteriaBuilder =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery =
                criteriaBuilder.createQuery(Long.class);

        Root<Project> projectRoot =
                countQuery.from(Project.class);

        List<Predicate> predicates =
                new ArrayList<>();

        predicates.add(
                criteriaBuilder.isFalse(
                        projectRoot.get("isDeleted")
                )
        );

        predicates.add(
                criteriaBuilder.isFalse(
                        projectRoot.get("isCancelled")
                )
        );

        if (statusName != null) {
            predicates.add(
                    criteriaBuilder.equal(
                            criteriaBuilder.upper(
                                    projectRoot
                                            .get("status")
                                            .get("name")
                            ),
                            statusName
                    )
            );
        }

        if (!fullAccess) {
            predicates.add(
                    buildProjectAccessPredicate(
                            requestingUser,
                            departmentManager,
                            departmentIds,
                            criteriaBuilder,
                            countQuery,
                            projectRoot
                    )
            );
        }

        countQuery
                .select(
                        criteriaBuilder.countDistinct(
                                projectRoot
                        )
                )
                .where(
                        criteriaBuilder.and(
                                predicates.toArray(new Predicate[0])
                        )
                );

        return entityManager
                .createQuery(countQuery)
                .getSingleResult();
    }
}