package com.doc.impl.research;

import com.doc.dto.research.*;
import com.doc.entity.product.Product;
import com.doc.entity.research.ResearchPriority;
import com.doc.entity.research.TechnicalResearchCase;
import com.doc.entity.research.TechnicalResearchCaseStatus;
import com.doc.entity.user.Role;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProductRepository;
import com.doc.repository.UserRepository;

import com.doc.repository.research.TechnicalResearchCaseRepository;
import com.doc.service.research.TechnicalResearchCaseService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TechnicalResearchCaseServiceImpl
        implements TechnicalResearchCaseService {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    TechnicalResearchCaseServiceImpl.class
            );


    private static final Set<TechnicalResearchCaseStatus>
            ACTIVE_ASSIGNMENT_STATUSES = Set.of(
            TechnicalResearchCaseStatus.ASSIGNED,
            TechnicalResearchCaseStatus.IN_PROGRESS,
            TechnicalResearchCaseStatus.AWAITING_INFORMATION,
            TechnicalResearchCaseStatus.UNDER_REVIEW,
            TechnicalResearchCaseStatus.REVISION_REQUIRED
    );

    private static final Set<TechnicalResearchCaseStatus>
            CLOSED_STATUSES = Set.of(
            TechnicalResearchCaseStatus.COMPLETED,
            TechnicalResearchCaseStatus.REJECTED,
            TechnicalResearchCaseStatus.CANCELLED
    );

    private static final Map<
            TechnicalResearchCaseStatus,
            Set<TechnicalResearchCaseStatus>
            > ALLOWED_STATUS_TRANSITIONS = Map.of(

            TechnicalResearchCaseStatus.PENDING_ASSIGNMENT,
            EnumSet.of(
                    TechnicalResearchCaseStatus.CANCELLED
            ),

            TechnicalResearchCaseStatus.IN_PROGRESS,
            EnumSet.of(
                    TechnicalResearchCaseStatus.AWAITING_INFORMATION,
                    TechnicalResearchCaseStatus.UNDER_REVIEW,
                    TechnicalResearchCaseStatus.COMPLETED,
                    TechnicalResearchCaseStatus.CANCELLED
            ),

            TechnicalResearchCaseStatus.IN_PROGRESS,
            EnumSet.of(
                    TechnicalResearchCaseStatus.AWAITING_INFORMATION,
                    TechnicalResearchCaseStatus.UNDER_REVIEW,
                    TechnicalResearchCaseStatus.CANCELLED
            ),

            TechnicalResearchCaseStatus.AWAITING_INFORMATION,
            EnumSet.of(
                    TechnicalResearchCaseStatus.IN_PROGRESS,
                    TechnicalResearchCaseStatus.CANCELLED
            ),

            TechnicalResearchCaseStatus.UNDER_REVIEW,
            EnumSet.of(
                    TechnicalResearchCaseStatus.REVISION_REQUIRED,
                    TechnicalResearchCaseStatus.COMPLETED,
                    TechnicalResearchCaseStatus.REJECTED,
                    TechnicalResearchCaseStatus.CANCELLED
            ),

            TechnicalResearchCaseStatus.REVISION_REQUIRED,
            EnumSet.of(
                    TechnicalResearchCaseStatus.IN_PROGRESS,
                    TechnicalResearchCaseStatus.UNDER_REVIEW,
                    TechnicalResearchCaseStatus.CANCELLED
            )
    );


    private final TechnicalResearchCaseRepository researchCaseRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public TechnicalResearchCaseServiceImpl(
            TechnicalResearchCaseRepository researchCaseRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this.researchCaseRepository = researchCaseRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public TechnicalResearchCaseResponseDto createCase(
            TechnicalResearchCaseCreateRequestDto request
    ) {
        logger.info(
                "Creating technical research case. productId={}, raisedBy={}",
                request.getProductId(),
                request.getRaisedByUserId()
        );

        Product product = getActiveProduct(request.getProductId());
        User raisedBy = getActiveUser(
                request.getRaisedByUserId(),
                "Salesperson"
        );

        TechnicalResearchCase researchCase =
                new TechnicalResearchCase();

        researchCase.setCaseNumber(generateCaseNumber());

        researchCase.setOriginatingLeadId(
                request.getOriginatingLeadId()
        );


        researchCase.setProduct(product);
        researchCase.setSubject(request.getSubject().trim());
        researchCase.setBusinessContext(
                trimToNull(request.getBusinessContext())
        );
        researchCase.setResearchScope(
                trimToNull(request.getResearchScope())
        );


        researchCase.setRaisedBy(raisedBy);
        researchCase.setCreatedBy(raisedBy);
        researchCase.setUpdatedBy(raisedBy);

        researchCase.setPriority(
                request.getPriority() != null
                        ? request.getPriority()
                        : ResearchPriority.MEDIUM
        );

        validateDueDate(request.getDueDate());
        researchCase.setDueDate(request.getDueDate());

        researchCase.setStatus(
                TechnicalResearchCaseStatus.PENDING_ASSIGNMENT
        );

        TechnicalResearchCase savedCase =
                researchCaseRepository.save(researchCase);

        logger.info(
                "Technical research case created. caseId={}, caseNumber={}",
                savedCase.getId(),
                savedCase.getCaseNumber()
        );

        return mapToResponseDto(savedCase);
    }



    @Override
    @Transactional(readOnly = true)
    public TechnicalResearchCaseResponseDto getCaseById(
            Long caseId
    ) {
        return mapToResponseDto(getCase(caseId));
    }


    @Override
    @Transactional(readOnly = true)
    public Page<TechnicalResearchCaseResponseDto> getCases(
            Long userId,
            TechnicalResearchCaseStatus status,
            ResearchPriority priority,
            String search,
            Pageable pageable
    ) {
        User user = getActiveUser(userId, "User");

        boolean canViewAllCases =
                hasOperationHeadRole(user)
                        || hasAdminRole(user);

        Specification<TechnicalResearchCase> specification =
                buildCaseSpecification(
                        userId,
                        canViewAllCases,
                        status,
                        priority,
                        search
                );

        logger.info(
                "Fetching research cases. userId={}, canViewAll={}, "
                        + "status={}, priority={}, productId={}, search={}",
                userId,
                canViewAllCases,
                status,
                priority,
                search
        );

        return researchCaseRepository
                .findAll(specification, pageable)
                .map(this::mapToResponseDto);
    }

    private Specification<TechnicalResearchCase> buildCaseSpecification(
            Long userId,
            boolean canViewAllCases,
            TechnicalResearchCaseStatus status,
            ResearchPriority priority,
            String search
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    criteriaBuilder.isFalse(root.get("deleted"))
            );

            /*
             * OPERATION_HEAD and ADMIN can see every case.
             *
             * Other users only see:
             * 1. Cases raised by them, or
             * 2. Cases currently assigned to them.
             */
            if (!canViewAllCases) {
                Join<TechnicalResearchCase, User> raisedByJoin =
                        root.join("raisedBy", JoinType.INNER);

                Join<TechnicalResearchCase, User> assigneeJoin =
                        root.join("currentAssignee", JoinType.LEFT);

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.equal(
                                        raisedByJoin.get("id"),
                                        userId
                                ),
                                criteriaBuilder.equal(
                                        assigneeJoin.get("id"),
                                        userId
                                )
                        )
                );
            }

            if (status != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                status
                        )
                );
            }

            if (priority != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("priority"),
                                priority
                        )
                );
            }


            if (StringUtils.hasText(search)) {
                String searchPattern =
                        "%" + search.trim()
                                .toLowerCase(Locale.ROOT) + "%";

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("caseNumber")
                                        ),
                                        searchPattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("subject")
                                        ),
                                        searchPattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("businessContext")
                                        ),
                                        searchPattern
                                )
                        )
                );
            }

            query.distinct(true);

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }


    private boolean hasOperationHeadRole(User user) {
        return hasRole(
                user,
                "OPERATION_HEAD",
                "ROLE_OPERATION_HEAD"
        );
    }



    private boolean hasRole(
            User user,
            String... allowedRoles
    ) {
        if (user.getRoles() == null
                || user.getRoles().isEmpty()) {
            return false;
        }

        Set<String> allowedRoleSet = Arrays.stream(allowedRoles)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return user.getRoles()
                .stream()
                .filter(Objects::nonNull)
                .filter(role -> !role.isDeleted())
                .map(Role::getName)
                .filter(StringUtils::hasText)
                .map(roleName ->
                        roleName.trim().toUpperCase(Locale.ROOT)
                )
                .anyMatch(allowedRoleSet::contains);
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveAssignmentCount(Long assigneeUserId) {
        getActiveUser(assigneeUserId, "Assignee");

        Specification<TechnicalResearchCase> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.and(
                                criteriaBuilder.isFalse(
                                        root.get("deleted")
                                ),
                                criteriaBuilder.equal(
                                        root.get("currentAssignee")
                                                .get("id"),
                                        assigneeUserId
                                ),
                                root.get("status").in(
                                        ACTIVE_ASSIGNMENT_STATUSES
                                )
                        );

        return researchCaseRepository.count(specification);
    }


    private TechnicalResearchCase getCase(Long caseId) {
        return researchCaseRepository
                .findByIdAndDeletedFalse(caseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Technical research case not found",
                        "ERR_RESEARCH_CASE_NOT_FOUND"
                ));
    }

    private User getActiveUser(
            Long userId,
            String userType
    ) {
        if (userId == null) {
            throw new ValidationException(
                    userType + " user ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        return userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        userType + " not found or inactive",
                        "ERR_USER_NOT_FOUND"
                ));
    }

    private Product getActiveProduct(Long productId) {
        if (productId == null) {
            throw new ValidationException(
                    "Product ID is required",
                    "ERR_PRODUCT_ID_REQUIRED"
            );
        }

        return productRepository.findActiveUserById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found or inactive",
                        "ERR_PRODUCT_NOT_FOUND"
                ));
    }



    private String normalizeRoleName(String roleName) {
        String normalized = roleName
                .trim()
                .toUpperCase(Locale.ROOT);

        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        return normalized;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TechnicalResearchCaseResponseDto> getCasesForUser(
            Long userId,
            Pageable pageable
    ) {
        User user = getActiveUser(userId, "User");

        logger.info(
                "Fetching technical research cases for userId={}, userName={}",
                user.getId(),
                user.getFullName()
        );

        return researchCaseRepository
                .findCasesForUser(userId, pageable)
                .map(this::mapToResponseDto);
    }


    private void validateDueDate(LocalDate dueDate) {
        if (dueDate != null
                && dueDate.isBefore(LocalDate.now(ZoneOffset.UTC))) {
            throw new ValidationException(
                    "Due date cannot be in the past",
                    "ERR_INVALID_RESEARCH_DUE_DATE"
            );
        }
    }

    private String generateCaseNumber() {
        String datePart = LocalDate
                .now(ZoneOffset.UTC)
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        String caseNumber;

        do {
            String randomPart = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase(Locale.ROOT);

            caseNumber = "TRC-" + datePart + "-" + randomPart;
        } while (researchCaseRepository
                .existsByCaseNumber(caseNumber));

        return caseNumber;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }

    private TechnicalResearchCaseResponseDto mapToResponseDto(
            TechnicalResearchCase researchCase
    ) {
        User assignee = researchCase.getCurrentAssignee();
        User assignedBy = researchCase.getLastAssignedBy();
        User closedBy = researchCase.getClosedBy();

        return TechnicalResearchCaseResponseDto.builder()
                .id(researchCase.getId())
                .caseNumber(researchCase.getCaseNumber())

                .originatingLeadId(
                        researchCase.getOriginatingLeadId()
                )

                .productId(
                        researchCase.getProduct().getId()
                )
                .productName(
                        researchCase.getProduct().getProductName()
                )

                .subject(researchCase.getSubject())
                .businessContext(
                        researchCase.getBusinessContext()
                )
                .researchScope(
                        researchCase.getResearchScope()
                )


                .raisedByUserId(
                        researchCase.getRaisedBy().getId()
                )
                .raisedByName(
                        researchCase.getRaisedBy().getFullName()
                )

                .currentAssigneeUserId(
                        assignee != null ? assignee.getId() : null
                )
                .currentAssigneeName(
                        assignee != null
                                ? assignee.getFullName()
                                : null
                )

                .lastAssignedByUserId(
                        assignedBy != null
                                ? assignedBy.getId()
                                : null
                )
                .lastAssignedByName(
                        assignedBy != null
                                ? assignedBy.getFullName()
                                : null
                )

                .status(researchCase.getStatus())
                .priority(researchCase.getPriority())
                .dueDate(researchCase.getDueDate())

                .firstAssignedAt(
                        researchCase.getFirstAssignedAt()
                )
                .lastAssignedAt(
                        researchCase.getLastAssignedAt()
                )
                .assignmentCount(
                        researchCase.getAssignmentCount()
                )
                .workStartedAt(
                        researchCase.getWorkStartedAt()
                )

                .findings(researchCase.getFindings())
                .recommendation(
                        researchCase.getRecommendation()
                )
                .submittedAt(researchCase.getSubmittedAt())

                .closedAt(researchCase.getClosedAt())
                .closedByUserId(
                        closedBy != null ? closedBy.getId() : null
                )
                .closedByName(
                        closedBy != null
                                ? closedBy.getFullName()
                                : null
                )
                .closureReason(
                        researchCase.getClosureReason()
                )

                .createdAt(researchCase.getCreatedAt())
                .updatedAt(researchCase.getUpdatedAt())
                .build();
    }


    @Override
    @Transactional
    public TechnicalResearchCaseResponseDto assignCase(
            Long caseId,
            Long assigneeUserId,
            Long assignedByUserId
    ) {
        TechnicalResearchCase researchCase =
                researchCaseRepository
                        .findByIdForAssignment(caseId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Technical research case not found",
                                "ERR_RESEARCH_CASE_NOT_FOUND"
                        ));

        User assignee = getActiveUser(
                assigneeUserId,
                "Assignee"
        );

        User assignedBy = getActiveUser(
                assignedByUserId,
                "Assigning user"
        );

        validateCaseCanBeAssigned(researchCase);

        /*
         * The assigning user must either:
         * 1. Be the direct manager of the assignee, or
         * 2. Have the ADMIN role.
         */
        validateAssignmentAuthority(
                assignedBy,
                assignee
        );



        if (Objects.equals(
                assignee.getId(),
                assignedBy.getId()
        )) {
            throw new ValidationException(
                    "User cannot assign the case to themselves",
                    "ERR_SELF_ASSIGNMENT_NOT_ALLOWED"
            );
        }

        if (researchCase.getCurrentAssignee() != null
                && Objects.equals(
                researchCase.getCurrentAssignee().getId(),
                assignee.getId()
        )) {
            throw new ValidationException(
                    "Research case is already assigned to this user",
                    "ERR_RESEARCH_CASE_ALREADY_ASSIGNED"
            );
        }

        Instant assignedAt = Instant.now();

        /*
         * Set only once during the first assignment.
         */
        if (researchCase.getFirstAssignedAt() == null) {
            researchCase.setFirstAssignedAt(assignedAt);
        }

        /*
         * Updated during every assignment or reassignment.
         */
        researchCase.setCurrentAssignee(assignee);
        researchCase.setLastAssignedBy(assignedBy);
        researchCase.setLastAssignedAt(assignedAt);

        int currentCount =
                researchCase.getAssignmentCount() == null
                        ? 0
                        : researchCase.getAssignmentCount();

        researchCase.setAssignmentCount(currentCount + 1);
        researchCase.setStatus(
                TechnicalResearchCaseStatus.ASSIGNED
        );
        researchCase.setUpdatedBy(assignedBy);

        TechnicalResearchCase savedCase =
                researchCaseRepository.save(researchCase);

        logger.info(
                "Research case assigned. caseId={}, "
                        + "assigneeUserId={}, assignedByUserId={}, "
                        + "assignmentCount={}",
                caseId,
                assigneeUserId,
                assignedByUserId,
                savedCase.getAssignmentCount()
        );

        return mapToResponseDto(savedCase);
    }


    private void validateCaseCanBeAssigned(
            TechnicalResearchCase researchCase
    ) {
        if (researchCase.getStatus()
                == TechnicalResearchCaseStatus.COMPLETED
                || researchCase.getStatus()
                == TechnicalResearchCaseStatus.REJECTED
                || researchCase.getStatus()
                == TechnicalResearchCaseStatus.CANCELLED) {

            throw new ValidationException(
                    "Completed, rejected or cancelled research case "
                            + "cannot be assigned",
                    "ERR_RESEARCH_CASE_CLOSED"
            );
        }
    }


    private boolean hasAdminRole(User user) {
        if (user.getRoles() == null
                || user.getRoles().isEmpty()) {
            return false;
        }

        return user.getRoles()
                .stream()
                .filter(Objects::nonNull)
                .filter(role -> !role.isDeleted())
                .map(Role::getName)
                .filter(StringUtils::hasText)
                .map(roleName ->
                        roleName.trim().toUpperCase(Locale.ROOT)
                )
                .anyMatch(roleName ->
                        roleName.equals("ADMIN")
                                || roleName.equals("ROLE_ADMIN")
                                || roleName.equals("OPERATION_HEAD")
                );
    }

    private void validateAssignmentAuthority(
            User assignedBy,
            User assignee
    ) {
        /*
         * ADMIN can assign a case to any eligible user.
         */
        if (hasAdminRole(assignedBy)) {
            return;
        }

        /*
         * Otherwise, the assigning user must be the
         * direct manager of the selected assignee.
         */
        boolean directManager =
                assignee.getManager() != null
                        && Objects.equals(
                        assignee.getManager().getId(),
                        assignedBy.getId()
                );

        if (!directManager) {
            throw new ValidationException(
                    "Only the assignee's direct manager "
                            + "or an ADMIN can assign this case",
                    "ERR_RESEARCH_ASSIGNMENT_ACCESS_DENIED"
            );
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Page<TechnicalResearchCaseResponseDto> getCasesByLeadId(
            Long leadId,
            Pageable pageable
    ) {
        logger.info(
                "Fetching technical research cases for leadId={}",
                leadId
        );

        return researchCaseRepository
                .findByOriginatingLeadIdAndDeletedFalse(
                        leadId,
                        pageable
                )
                .map(this::mapToResponseDto);
    }

    @Override
    @Transactional
    public TechnicalResearchCaseResponseDto updateStatus(
            Long caseId,
            TechnicalResearchCaseStatusUpdateRequestDto request
    ) {
        logger.info(
                "Research status update started. caseId={}, requestedStatus={}, "
                        + "updatedByUserId={}",
                caseId,
                request != null ? request.getStatus() : null,
                request != null ? request.getUpdatedByUserId() : null
        );

        if (request == null) {
            logger.warn(
                    "Research status update failed because request is null. "
                            + "caseId={}",
                    caseId
            );

            throw new ValidationException(
                    "Status update request is required",
                    "ERR_RESEARCH_STATUS_REQUEST_REQUIRED"
            );
        }

        logger.info(
                "Fetching technical research case with lock. caseId={}",
                caseId
        );

        TechnicalResearchCase researchCase =
                researchCaseRepository
                        .findByIdForStatusUpdate(caseId)
                        .orElseThrow(() -> {
                            logger.warn(
                                    "Technical research case not found. caseId={}",
                                    caseId
                            );

                            return new ResourceNotFoundException(
                                    "Technical research case not found",
                                    "ERR_RESEARCH_CASE_NOT_FOUND"
                            );
                        });

        logger.info(
                "Research case loaded. caseId={}, caseNumber={}, "
                        + "currentStatus={}, currentAssigneeUserId={}",
                researchCase.getId(),
                researchCase.getCaseNumber(),
                researchCase.getStatus(),
                researchCase.getCurrentAssignee() != null
                        ? researchCase.getCurrentAssignee().getId()
                        : null
        );

        User updatedBy = getActiveUser(
                request.getUpdatedByUserId(),
                "Status updating user"
        );

        logger.info(
                "Status updating user validated. caseId={}, "
                        + "updatedByUserId={}, updatedByName={}",
                caseId,
                updatedBy.getId(),
                updatedBy.getFullName()
        );

        TechnicalResearchCaseStatus currentStatus =
                researchCase.getStatus();

        TechnicalResearchCaseStatus newStatus =
                request.getStatus();

        String reason = trimToNull(request.getReason());

        if (newStatus == null) {
            logger.warn(
                    "Research status update rejected because new status is null. "
                            + "caseId={}, updatedByUserId={}",
                    caseId,
                    updatedBy.getId()
            );

            throw new ValidationException(
                    "New status is required",
                    "ERR_RESEARCH_STATUS_REQUIRED"
            );
        }

        logger.info(
                "Validating research case status transition. "
                        + "caseId={}, currentStatus={}, newStatus={}",
                caseId,
                currentStatus,
                newStatus
        );

        validateStatusTransition(
                currentStatus,
                newStatus
        );

        logger.info(
                "Status transition validated successfully. "
                        + "caseId={}, currentStatus={}, newStatus={}",
                caseId,
                currentStatus,
                newStatus
        );

        logger.info(
                "Validating status update authority. "
                        + "caseId={}, updatedByUserId={}, newStatus={}",
                caseId,
                updatedBy.getId(),
                newStatus
        );

        validateStatusUpdateAuthority(
                researchCase,
                updatedBy,
                newStatus
        );

        logger.info(
                "Status update authority validated. "
                        + "caseId={}, updatedByUserId={}, newStatus={}",
                caseId,
                updatedBy.getId(),
                newStatus
        );

        /*
         * Reason is mandatory when the case is moved to any final status.
         */
        if (CLOSED_STATUSES.contains(newStatus)
                && reason == null) {

            logger.warn(
                    "Research case closure rejected because reason is missing. "
                            + "caseId={}, currentStatus={}, newStatus={}, "
                            + "updatedByUserId={}",
                    caseId,
                    currentStatus,
                    newStatus,
                    updatedBy.getId()
            );

            throw new ValidationException(
                    "Reason is required when completing, "
                            + "rejecting or cancelling a research case",
                    "ERR_RESEARCH_CLOSURE_REASON_REQUIRED"
            );
        }

        Instant now = Instant.now();

        researchCase.setStatus(newStatus);
        researchCase.setUpdatedBy(updatedBy);

        logger.info(
                "Applying status-specific changes. "
                        + "caseId={}, newStatus={}",
                caseId,
                newStatus
        );

        switch (newStatus) {
            case IN_PROGRESS -> {
                if (researchCase.getWorkStartedAt() == null) {
                    researchCase.setWorkStartedAt(now);

                    logger.info(
                            "Research work started timestamp recorded. "
                                    + "caseId={}, workStartedAt={}",
                            caseId,
                            now
                    );
                } else {
                    logger.info(
                            "Research work was already started. "
                                    + "caseId={}, existingWorkStartedAt={}",
                            caseId,
                            researchCase.getWorkStartedAt()
                    );
                }
            }

            case AWAITING_INFORMATION -> logger.info(
                    "Research case is waiting for additional information. "
                            + "caseId={}",
                    caseId
            );

            case UNDER_REVIEW -> {
                researchCase.setSubmittedAt(now);

                logger.info(
                        "Research case submitted for review. "
                                + "caseId={}, submittedAt={}",
                        caseId,
                        now
                );
            }

            case REVISION_REQUIRED -> logger.info(
                    "Research case sent back for revision. caseId={}",
                    caseId
            );

            case COMPLETED -> {
                researchCase.setClosedAt(now);
                researchCase.setClosedBy(updatedBy);
                researchCase.setClosureReason(reason);

                /*
                 * When directly completing from IN_PROGRESS,
                 * treat the same time as the submission time if the
                 * case was never submitted for review.
                 */
                if (researchCase.getSubmittedAt() == null) {
                    researchCase.setSubmittedAt(now);
                }

                logger.info(
                        "Research case completed. caseId={}, "
                                + "closedByUserId={}, closedAt={}, "
                                + "closureReasonProvided={}",
                        caseId,
                        updatedBy.getId(),
                        now,
                        true
                );
            }

            case REJECTED -> {
                researchCase.setClosedAt(now);
                researchCase.setClosedBy(updatedBy);
                researchCase.setClosureReason(reason);

                logger.info(
                        "Research case rejected. caseId={}, "
                                + "closedByUserId={}, closedAt={}, "
                                + "closureReasonProvided={}",
                        caseId,
                        updatedBy.getId(),
                        now,
                        true
                );
            }

            case CANCELLED -> {
                researchCase.setClosedAt(now);
                researchCase.setClosedBy(updatedBy);
                researchCase.setClosureReason(reason);

                logger.info(
                        "Research case cancelled. caseId={}, "
                                + "closedByUserId={}, closedAt={}, "
                                + "closureReasonProvided={}",
                        caseId,
                        updatedBy.getId(),
                        now,
                        true
                );
            }

            default -> logger.info(
                    "No additional fields required for status. "
                            + "caseId={}, newStatus={}",
                    caseId,
                    newStatus
            );
        }

        logger.info(
                "Saving research case status update. "
                        + "caseId={}, previousStatus={}, newStatus={}",
                caseId,
                currentStatus,
                newStatus
        );

        TechnicalResearchCase savedCase =
                researchCaseRepository.save(researchCase);

        logger.info(
                "Research case status update completed successfully. "
                        + "caseId={}, caseNumber={}, previousStatus={}, "
                        + "newStatus={}, updatedByUserId={}, closedAt={}",
                savedCase.getId(),
                savedCase.getCaseNumber(),
                currentStatus,
                savedCase.getStatus(),
                updatedBy.getId(),
                savedCase.getClosedAt()
        );

        return mapToResponseDto(savedCase);
    }

    private void validateStatusTransition(
            TechnicalResearchCaseStatus currentStatus,
            TechnicalResearchCaseStatus newStatus
    ) {
        logger.debug(
                "Checking status transition. currentStatus={}, newStatus={}",
                currentStatus,
                newStatus
        );

        if (currentStatus == null) {
            logger.warn(
                    "Status transition rejected because current status is null. "
                            + "newStatus={}",
                    newStatus
            );

            throw new ValidationException(
                    "Current research case status is unavailable",
                    "ERR_RESEARCH_CURRENT_STATUS_MISSING"
            );
        }

        if (currentStatus == newStatus) {
            logger.warn(
                    "Status transition rejected because status is unchanged. "
                            + "status={}",
                    currentStatus
            );

            throw new ValidationException(
                    "Research case is already in status: "
                            + newStatus.getDisplayName(),
                    "ERR_RESEARCH_STATUS_ALREADY_SET"
            );
        }

        if (CLOSED_STATUSES.contains(currentStatus)) {
            logger.warn(
                    "Status transition rejected because case is already closed. "
                            + "currentStatus={}, requestedStatus={}",
                    currentStatus,
                    newStatus
            );

            throw new ValidationException(
                    "Status of a completed, rejected or cancelled "
                            + "research case cannot be changed",
                    "ERR_RESEARCH_CASE_ALREADY_CLOSED"
            );
        }

        Set<TechnicalResearchCaseStatus> allowedStatuses =
                ALLOWED_STATUS_TRANSITIONS.getOrDefault(
                        currentStatus,
                        Collections.emptySet()
                );

        if (!allowedStatuses.contains(newStatus)) {
            logger.warn(
                    "Invalid research case status transition. "
                            + "currentStatus={}, requestedStatus={}, "
                            + "allowedStatuses={}",
                    currentStatus,
                    newStatus,
                    allowedStatuses
            );

            throw new ValidationException(
                    "Status cannot be changed from "
                            + currentStatus.getDisplayName()
                            + " to "
                            + newStatus.getDisplayName(),
                    "ERR_INVALID_RESEARCH_STATUS_TRANSITION"
            );
        }

        logger.debug(
                "Research status transition allowed. "
                        + "currentStatus={}, newStatus={}",
                currentStatus,
                newStatus
        );
    }

    private void validateStatusUpdateAuthority(
            TechnicalResearchCase researchCase,
            User updatedBy,
            TechnicalResearchCaseStatus newStatus
    ) {
        boolean adminOrOperationHead =
                hasAdminRole(updatedBy)
                        || hasOperationHeadRole(updatedBy);

        boolean currentAssignee =
                researchCase.getCurrentAssignee() != null
                        && Objects.equals(
                        researchCase.getCurrentAssignee().getId(),
                        updatedBy.getId()
                );

        boolean assigneeManager =
                researchCase.getCurrentAssignee() != null
                        && researchCase.getCurrentAssignee().getManager() != null
                        && Objects.equals(
                        researchCase
                                .getCurrentAssignee()
                                .getManager()
                                .getId(),
                        updatedBy.getId()
                );

        boolean raisedByUser =
                researchCase.getRaisedBy() != null
                        && Objects.equals(
                        researchCase.getRaisedBy().getId(),
                        updatedBy.getId()
                );

        logger.debug(
                "Status authority details. caseId={}, updatedByUserId={}, "
                        + "newStatus={}, adminOrOperationHead={}, "
                        + "currentAssignee={}, assigneeManager={}, raisedByUser={}",
                researchCase.getId(),
                updatedBy.getId(),
                newStatus,
                adminOrOperationHead,
                currentAssignee,
                assigneeManager,
                raisedByUser
        );

        if (adminOrOperationHead) {
            return;
        }

        /*
         * Assigned technical user can perform the complete working flow,
         * including direct completion from IN_PROGRESS.
         */
        if (newStatus == TechnicalResearchCaseStatus.IN_PROGRESS
                || newStatus
                == TechnicalResearchCaseStatus.AWAITING_INFORMATION
                || newStatus
                == TechnicalResearchCaseStatus.UNDER_REVIEW
                || newStatus
                == TechnicalResearchCaseStatus.COMPLETED) {

            if (currentAssignee) {
                return;
            }

            logger.warn(
                    "Status update denied because user is not current assignee. "
                            + "caseId={}, updatedByUserId={}, newStatus={}",
                    researchCase.getId(),
                    updatedBy.getId(),
                    newStatus
            );

            throw new ValidationException(
                    "Only the current assignee can update "
                            + "the case to this status",
                    "ERR_RESEARCH_STATUS_ACCESS_DENIED"
            );
        }

        if (newStatus == TechnicalResearchCaseStatus.REVISION_REQUIRED
                || newStatus == TechnicalResearchCaseStatus.REJECTED) {

            if (assigneeManager) {
                return;
            }

            logger.warn(
                    "Review decision denied. caseId={}, "
                            + "updatedByUserId={}, newStatus={}",
                    researchCase.getId(),
                    updatedBy.getId(),
                    newStatus
            );

            throw new ValidationException(
                    "Only the assignee's manager, Operation Head "
                            + "or Admin can perform this review decision",
                    "ERR_RESEARCH_REVIEW_ACCESS_DENIED"
            );
        }

        if (newStatus == TechnicalResearchCaseStatus.CANCELLED
                && (raisedByUser || currentAssignee || assigneeManager)) {
            return;
        }

        logger.warn(
                "Research status update access denied. "
                        + "caseId={}, updatedByUserId={}, newStatus={}",
                researchCase.getId(),
                updatedBy.getId(),
                newStatus
        );

        throw new ValidationException(
                "You are not authorized to update this research case status",
                "ERR_RESEARCH_STATUS_ACCESS_DENIED"
        );
    }


}