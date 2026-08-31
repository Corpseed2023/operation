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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class TechnicalResearchCaseServiceImpl
        implements TechnicalResearchCaseService {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    TechnicalResearchCaseServiceImpl.class
            );

    private static final Set<String> MANAGEMENT_ROLES = Set.of(
            "ADMIN",
            "SUPER_ADMIN",
            "OPERATION_HEAD",
            "OPERATIONS_HEAD",
            "TECHNICAL_HEAD",
            "MANAGER"
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
        researchCase.setOriginatingSolutionId(
                request.getOriginatingSolutionId()
        );
        researchCase.setSolutionNameSnapshot(
                trimToNull(request.getSolutionName())
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
    @Transactional
    public TechnicalResearchCaseResponseDto assignCase(
            Long caseId,
            TechnicalResearchAssignmentRequestDto request
    ) {
        TechnicalResearchCase researchCase = getCase(caseId);
        User assignedBy = getActiveUser(
                request.getAssignedByUserId(),
                "Assigning manager"
        );
        User assignee = getActiveUser(
                request.getAssigneeUserId(),
                "Assignee"
        );

        validateManagementAuthority(assignedBy);
        validateCaseIsOpen(researchCase);
        validateDueDate(request.getDueDate());

        if (Objects.equals(
                assignedBy.getId(),
                assignee.getId()
        )) {
            throw new ValidationException(
                    "Manager cannot assign the research case to themselves",
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
                    "ERR_CASE_ALREADY_ASSIGNED_TO_USER"
            );
        }

        validateProductMapping(
                assignee,
                researchCase.getProduct()
        );

        Instant now = Instant.now();

        if (researchCase.getFirstAssignedAt() == null) {
            researchCase.setFirstAssignedAt(now);
        }

        researchCase.setCurrentAssignee(assignee);
        researchCase.setLastAssignedBy(assignedBy);
        researchCase.setLastAssignedAt(now);

        int currentAssignmentCount =
                researchCase.getAssignmentCount() == null
                        ? 0
                        : researchCase.getAssignmentCount();

        researchCase.setAssignmentCount(
                currentAssignmentCount + 1
        );

        if (request.getDueDate() != null) {
            researchCase.setDueDate(request.getDueDate());
        }

        /*
         * A reassigned case returns to ASSIGNED so the new
         * technical person must explicitly start the work.
         */
        researchCase.setStatus(
                TechnicalResearchCaseStatus.ASSIGNED
        );
        researchCase.setUpdatedBy(assignedBy);

        TechnicalResearchCase savedCase =
                researchCaseRepository.save(researchCase);

        logger.info(
                "Research case assigned. caseId={}, assigneeId={}, "
                        + "assignedBy={}, assignmentCount={}",
                caseId,
                assignee.getId(),
                assignedBy.getId(),
                savedCase.getAssignmentCount()
        );

        return mapToResponseDto(savedCase);
    }

    @Override
    @Transactional
    public TechnicalResearchCaseResponseDto startWork(
            Long caseId,
            TechnicalResearchActionRequestDto request
    ) {
        TechnicalResearchCase researchCase = getCase(caseId);
        User actor = getActiveUser(
                request.getActorUserId(),
                "Technical user"
        );

        validateCurrentAssignee(researchCase, actor);

        if (researchCase.getStatus()
                != TechnicalResearchCaseStatus.ASSIGNED
                && researchCase.getStatus()
                != TechnicalResearchCaseStatus.REVISION_REQUIRED) {

            throw invalidTransition(
                    researchCase,
                    TechnicalResearchCaseStatus.IN_PROGRESS
            );
        }

        researchCase.setStatus(
                TechnicalResearchCaseStatus.IN_PROGRESS
        );

        if (researchCase.getWorkStartedAt() == null) {
            researchCase.setWorkStartedAt(Instant.now());
        }

        researchCase.setUpdatedBy(actor);

        return mapToResponseDto(
                researchCaseRepository.save(researchCase)
        );
    }

    @Override
    @Transactional
    public TechnicalResearchCaseResponseDto submitCase(
            Long caseId,
            TechnicalResearchSubmissionRequestDto request
    ) {
        TechnicalResearchCase researchCase = getCase(caseId);
        User actor = getActiveUser(
                request.getSubmittedByUserId(),
                "Technical user"
        );

        validateCurrentAssignee(researchCase, actor);

        if (researchCase.getStatus()
                != TechnicalResearchCaseStatus.IN_PROGRESS) {
            throw invalidTransition(
                    researchCase,
                    TechnicalResearchCaseStatus.UNDER_REVIEW
            );
        }

        researchCase.setFindings(request.getFindings().trim());
        researchCase.setRecommendation(
                trimToNull(request.getRecommendation())
        );
        researchCase.setSubmittedAt(Instant.now());
        researchCase.setStatus(
                TechnicalResearchCaseStatus.UNDER_REVIEW
        );
        researchCase.setUpdatedBy(actor);

        TechnicalResearchCase savedCase =
                researchCaseRepository.save(researchCase);

        logger.info(
                "Research case submitted. caseId={}, submittedBy={}",
                caseId,
                actor.getId()
        );

        return mapToResponseDto(savedCase);
    }

    @Override
    @Transactional
    public TechnicalResearchCaseResponseDto requestRevision(
            Long caseId,
            TechnicalResearchClosureRequestDto request
    ) {
        TechnicalResearchCase researchCase = getCase(caseId);
        User reviewer = getActiveUser(
                request.getActorUserId(),
                "Reviewer"
        );

        validateManagementAuthority(reviewer);

        if (researchCase.getStatus()
                != TechnicalResearchCaseStatus.UNDER_REVIEW) {
            throw invalidTransition(
                    researchCase,
                    TechnicalResearchCaseStatus.REVISION_REQUIRED
            );
        }

        researchCase.setClosureReason(request.getReason().trim());
        researchCase.setStatus(
                TechnicalResearchCaseStatus.REVISION_REQUIRED
        );
        researchCase.setUpdatedBy(reviewer);

        return mapToResponseDto(
                researchCaseRepository.save(researchCase)
        );
    }

    @Override
    @Transactional
    public TechnicalResearchCaseResponseDto completeCase(
            Long caseId,
            TechnicalResearchActionRequestDto request
    ) {
        TechnicalResearchCase researchCase = getCase(caseId);
        User reviewer = getActiveUser(
                request.getActorUserId(),
                "Reviewer"
        );

        validateManagementAuthority(reviewer);

        if (researchCase.getStatus()
                != TechnicalResearchCaseStatus.UNDER_REVIEW) {
            throw invalidTransition(
                    researchCase,
                    TechnicalResearchCaseStatus.COMPLETED
            );
        }

        if (!StringUtils.hasText(researchCase.getFindings())) {
            throw new ValidationException(
                    "Research findings are required before completion",
                    "ERR_RESEARCH_FINDINGS_REQUIRED"
            );
        }

        researchCase.setStatus(
                TechnicalResearchCaseStatus.COMPLETED
        );
        researchCase.setClosedAt(Instant.now());
        researchCase.setClosedBy(reviewer);
        researchCase.setClosureReason(null);
        researchCase.setUpdatedBy(reviewer);

        TechnicalResearchCase savedCase =
                researchCaseRepository.save(researchCase);

        logger.info(
                "Research case completed. caseId={}, completedBy={}",
                caseId,
                reviewer.getId()
        );

        return mapToResponseDto(savedCase);
    }

    @Override
    @Transactional
    public TechnicalResearchCaseResponseDto rejectCase(
            Long caseId,
            TechnicalResearchClosureRequestDto request
    ) {
        TechnicalResearchCase researchCase = getCase(caseId);
        User actor = getActiveUser(
                request.getActorUserId(),
                "Rejecting user"
        );

        validateManagementAuthority(actor);
        validateCaseIsOpen(researchCase);

        closeCase(
                researchCase,
                actor,
                TechnicalResearchCaseStatus.REJECTED,
                request.getReason()
        );

        return mapToResponseDto(
                researchCaseRepository.save(researchCase)
        );
    }

    @Override
    @Transactional
    public TechnicalResearchCaseResponseDto cancelCase(
            Long caseId,
            TechnicalResearchClosureRequestDto request
    ) {
        TechnicalResearchCase researchCase = getCase(caseId);
        User actor = getActiveUser(
                request.getActorUserId(),
                "Cancelling user"
        );

        validateCaseIsOpen(researchCase);

        boolean raisedByActor = Objects.equals(
                researchCase.getRaisedBy().getId(),
                actor.getId()
        );

        if (!raisedByActor && !hasManagementAuthority(actor)) {
            throw new ValidationException(
                    "Only the salesperson who raised the case "
                            + "or an authorized manager can cancel it",
                    "ERR_CASE_CANCELLATION_NOT_ALLOWED"
            );
        }

        closeCase(
                researchCase,
                actor,
                TechnicalResearchCaseStatus.CANCELLED,
                request.getReason()
        );

        return mapToResponseDto(
                researchCaseRepository.save(researchCase)
        );
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
            TechnicalResearchCaseStatus status,
            ResearchPriority priority,
            Long productId,
            Long raisedByUserId,
            Long assigneeUserId,
            String search,
            Pageable pageable
    ) {
        Specification<TechnicalResearchCase> specification =
                buildSpecification(
                        status,
                        priority,
                        productId,
                        raisedByUserId,
                        assigneeUserId,
                        search
                );

        return researchCaseRepository
                .findAll(specification, pageable)
                .map(this::mapToResponseDto);
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

    private Specification<TechnicalResearchCase>
    buildSpecification(
            TechnicalResearchCaseStatus status,
            ResearchPriority priority,
            Long productId,
            Long raisedByUserId,
            Long assigneeUserId,
            String search
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    criteriaBuilder.isFalse(root.get("deleted"))
            );

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

            if (productId != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("product").get("id"),
                                productId
                        )
                );
            }

            if (raisedByUserId != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("raisedBy").get("id"),
                                raisedByUserId
                        )
                );
            }

            if (assigneeUserId != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("currentAssignee").get("id"),
                                assigneeUserId
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
                                                root.get(
                                                        "solutionNameSnapshot"
                                                )
                                        ),
                                        searchPattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get(
                                                        "customerNameSnapshot"
                                                )
                                        ),
                                        searchPattern
                                )
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }

    private void closeCase(
            TechnicalResearchCase researchCase,
            User actor,
            TechnicalResearchCaseStatus status,
            String reason
    ) {
        if (!StringUtils.hasText(reason)) {
            throw new ValidationException(
                    "Closure reason is required",
                    "ERR_CLOSURE_REASON_REQUIRED"
            );
        }

        researchCase.setStatus(status);
        researchCase.setClosureReason(reason.trim());
        researchCase.setClosedAt(Instant.now());
        researchCase.setClosedBy(actor);
        researchCase.setUpdatedBy(actor);
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

    private void validateManagementAuthority(User user) {
        if (!hasManagementAuthority(user)) {
            throw new ValidationException(
                    "User is not authorized to manage research assignments",
                    "ERR_RESEARCH_MANAGEMENT_ACCESS_DENIED"
            );
        }
    }

    private boolean hasManagementAuthority(User user) {
        if (user.isManagerFlag()) {
            return true;
        }

        if (user.getRoles() == null) {
            return false;
        }

        return user.getRoles()
                .stream()
                .filter(Objects::nonNull)
                .filter(role -> !role.isDeleted())
                .map(Role::getName)
                .filter(StringUtils::hasText)
                .map(this::normalizeRoleName)
                .anyMatch(MANAGEMENT_ROLES::contains);
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

    /**
     * Ensures that the technical user is configured for the
     * product being researched.
     */
    private void validateProductMapping(
            User assignee,
            Product product
    ) {
        boolean mappedToProduct =
                assignee.getUserProductMaps() != null
                        && assignee.getUserProductMaps()
                        .stream()
                        .anyMatch(mapping ->
                                mapping != null
                                        && !mapping.isDeleted()
                                        && mapping.isAssigned()
                                        && mapping.getProduct() != null
                                        && Objects.equals(
                                        mapping.getProduct().getId(),
                                        product.getId()
                                )
                        );

        if (!mappedToProduct) {
            throw new ValidationException(
                    "Selected technical user is not mapped "
                            + "to product " + product.getProductName(),
                    "ERR_ASSIGNEE_PRODUCT_MAPPING_NOT_FOUND"
            );
        }
    }

    private void validateCurrentAssignee(
            TechnicalResearchCase researchCase,
            User actor
    ) {
        if (researchCase.getCurrentAssignee() == null) {
            throw new ValidationException(
                    "Research case has not been assigned",
                    "ERR_RESEARCH_CASE_NOT_ASSIGNED"
            );
        }

        if (!Objects.equals(
                researchCase.getCurrentAssignee().getId(),
                actor.getId()
        )) {
            throw new ValidationException(
                    "Only the currently assigned technical user "
                            + "can perform this action",
                    "ERR_RESEARCH_CASE_ASSIGNEE_MISMATCH"
            );
        }
    }

    private void validateCaseIsOpen(
            TechnicalResearchCase researchCase
    ) {
        if (CLOSED_STATUSES.contains(researchCase.getStatus())) {
            throw new ValidationException(
                    "Closed research case cannot be modified",
                    "ERR_RESEARCH_CASE_ALREADY_CLOSED"
            );
        }
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

    private ValidationException invalidTransition(
            TechnicalResearchCase researchCase,
            TechnicalResearchCaseStatus targetStatus
    ) {
        return new ValidationException(
                "Research case cannot move from "
                        + researchCase.getStatus()
                        + " to " + targetStatus,
                "ERR_INVALID_RESEARCH_STATUS_TRANSITION"
        );
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
                .originatingSolutionId(
                        researchCase.getOriginatingSolutionId()
                )
                .solutionName(
                        researchCase.getSolutionNameSnapshot()
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
}