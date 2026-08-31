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


}