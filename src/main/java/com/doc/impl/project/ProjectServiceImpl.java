package com.doc.impl.project;

import com.doc.constants.StatusConstants;
import com.doc.dto.contact.ContactDetailsDto;
import com.doc.dto.document.DocumentChecklistDTO;
import com.doc.dto.project.*;
import com.doc.dto.project.projectHistory.*;
import com.doc.em.ProjectHistoryEventType;
import com.doc.em.ProjectHistoryReferenceType;
import com.doc.dto.project.sales.DepartmentWiseMilestoneDto;
import com.doc.dto.project.sales.MilestoneAssignmentStatusDto;
import com.doc.dto.project.sales.SalesProjectStatusResponseDto;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.dto.user.UserResponseDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.CompanyUnit;
import com.doc.entity.client.Contact;
import com.doc.entity.client.PaymentType;
import com.doc.entity.department.Department;
import com.doc.entity.document.ApplicantType;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.milestone.Milestone;
import com.doc.entity.milestone.MilestoneStatus;
import com.doc.entity.milestone.MilestoneStatusHistory;
import com.doc.entity.project.*;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.user.User;
import com.doc.entity.vendor.ProcurementMilestoneAssignment;
import com.doc.entity.vendor.VendorStatus;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.feign.LeadFeignClient;
import com.doc.repository.*;
import com.doc.repository.documentRepo.ApplicantTypeRepository;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.repository.vendor.VendorRepository;
import com.doc.entity.vendor.Vendor;
import com.doc.service.AutoAssignmentService;
import com.doc.service.project.ProjectMailService;
import com.doc.service.project.ProjectService;
import com.doc.validator.request.ProjectRequestValidator;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final ProjectPaymentDetailRepository projectPaymentDetailRepository;
    private final ProjectPaymentTransactionRepository projectPaymentTransactionRepository;
    private final ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;
    private final ProjectAssignmentHistoryRepository projectAssignmentHistoryRepository;
    private final ProductMilestoneMapRepository productMilestoneMapRepository;
    private final ProjectDocumentUploadRepository projectDocumentUploadRepository;
    private final MilestoneStatusHistoryRepository milestoneStatusHistoryRepository;
    private final MilestoneStatusRepository milestoneStatusRepository;
    private final ProjectStatusRepository projectStatusRepository;
    private final AutoAssignmentService autoAssignmentService;
    private final ProjectRequestValidator projectRequestValidator;
    private final VendorRepository vendorRepository;
    private final CompanyUnitRepository companyUnitRepository;
    private final ProductDocumentMappingRepository productDocumentMappingRepository;
    private final ApplicantTypeRepository applicantTypeRepository;
    private final ProcurementMilestoneAssignmentRepository procurementMilestoneAssignmentRepository;
    private final ProjectMailService projectMailService;
    private final LeadFeignClient leadFeignClient;
    private final ProjectHistoryEventRepository projectHistoryEventRepository;

    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            CompanyRepository companyRepository,
            ContactRepository contactRepository,
            PaymentTypeRepository paymentTypeRepository,
            ProjectPaymentDetailRepository projectPaymentDetailRepository,
            ProjectPaymentTransactionRepository projectPaymentTransactionRepository,
            ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository,
            ProjectAssignmentHistoryRepository projectAssignmentHistoryRepository,
            ProductMilestoneMapRepository productMilestoneMapRepository,
            ProjectDocumentUploadRepository projectDocumentUploadRepository,
            MilestoneStatusHistoryRepository milestoneStatusHistoryRepository,
            MilestoneStatusRepository milestoneStatusRepository,
            ProjectStatusRepository projectStatusRepository,
            AutoAssignmentService autoAssignmentService,
            ProjectRequestValidator projectRequestValidator,
            VendorRepository vendorRepository,
            CompanyUnitRepository companyUnitRepository,
            ProductDocumentMappingRepository productDocumentMappingRepository,
            ApplicantTypeRepository applicantTypeRepository,
            ProcurementMilestoneAssignmentRepository procurementMilestoneAssignmentRepository,
            ProjectMailService projectMailService,
            LeadFeignClient leadFeignClient,
            ProjectHistoryEventRepository projectHistoryEventRepository
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
        this.paymentTypeRepository = paymentTypeRepository;
        this.projectPaymentDetailRepository = projectPaymentDetailRepository;
        this.projectPaymentTransactionRepository = projectPaymentTransactionRepository;
        this.projectMilestoneAssignmentRepository = projectMilestoneAssignmentRepository;
        this.projectAssignmentHistoryRepository = projectAssignmentHistoryRepository;
        this.productMilestoneMapRepository = productMilestoneMapRepository;
        this.projectDocumentUploadRepository = projectDocumentUploadRepository;
        this.milestoneStatusHistoryRepository = milestoneStatusHistoryRepository;
        this.milestoneStatusRepository = milestoneStatusRepository;
        this.projectStatusRepository = projectStatusRepository;
        this.autoAssignmentService = autoAssignmentService;
        this.projectRequestValidator = projectRequestValidator;
        this.vendorRepository = vendorRepository;
        this.companyUnitRepository = companyUnitRepository;
        this.productDocumentMappingRepository = productDocumentMappingRepository;
        this.applicantTypeRepository = applicantTypeRepository;
        this.procurementMilestoneAssignmentRepository = procurementMilestoneAssignmentRepository;
        this.projectMailService = projectMailService;
        this.leadFeignClient = leadFeignClient;
        this.projectHistoryEventRepository = projectHistoryEventRepository;
    }


    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ProjectResponseDto createProject(ProjectRequestDto requestDto) {
        logger.info("Creating project with projectNo: {}", requestDto.getProjectNo());
        projectRequestValidator.validate(requestDto);

        // Duplicate checks
        if (projectRepository.existsByProjectNoAndIsDeletedFalse(requestDto.getProjectNo().trim())) {
            throw new ValidationException("Project with number " + requestDto.getProjectNo() + " already exists", "ERR_DUPLICATE_PROJECT_NO");
        }
        if (StringUtils.hasText(requestDto.getUnbilledNumber()) &&
                projectRepository.existsByUnbilledNumberAndIsDeletedFalse(requestDto.getUnbilledNumber().trim())) {
            throw new ValidationException("Unbilled number already exists", "ERR_DUPLICATE_UNBILLED_NO");
        }

        if (StringUtils.hasText(requestDto.getEstimateNumber()) &&
                projectRepository.existsByEstimateNumberAndIsDeletedFalse(requestDto.getEstimateNumber().trim())) {
            throw new ValidationException("Estimate number already exists", "ERR_DUPLICATE_ESTIMATE_NO");
        }

        Product product = productRepository.findActiveUserById(requestDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found", "ERR_PRODUCT_NOT_FOUND"));
        Company company = companyRepository.findByIdAndIsDeletedFalse(requestDto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found or deleted", "ERR_COMPANY_NOT_FOUND"));

        CompanyUnit unit = null;
        if (requestDto.getUnitId() != null) {
            unit = companyUnitRepository.findByIdAndCompanyIdAndIsDeletedFalse(requestDto.getUnitId(), requestDto.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found or doesn't belong to company", "ERR_UNIT_NOT_FOUND"));
        }
        Contact contact = contactRepository.findByIdAndDeleteStatusFalseAndIsActiveTrueAndIsDeletedFalse(requestDto.getContactId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found, inactive or deleted", "ERR_CONTACT_NOT_FOUND"));
        User createdBy = userRepository.findActiveUserById(requestDto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));
        User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));
        User approvedBy = userRepository.findActiveUserById(requestDto.getApprovedById())
                .orElseThrow(() -> new ResourceNotFoundException("Approved by user not found", "ERR_APPROVED_BY_NOT_FOUND"));
        PaymentType paymentType = paymentTypeRepository.findById(requestDto.getPaymentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment type not found", "ERR_PAYMENT_TYPE_NOT_FOUND"));

        List<ProductMilestoneMap> milestones = getMilestoneMaps(product.getId());
        if (milestones.isEmpty()) {
            throw new ValidationException("No milestones defined for product ID " + product.getId(), "ERR_NO_MILESTONES");
        }

        double totalAmount = requestDto.getTotalAmount();
        double paidAmount = requestDto.getPaidAmount() != null ? requestDto.getPaidAmount() : 0.0;
        double dueAmount = totalAmount - paidAmount;

        String paymentTypeName = paymentType.getName();
        validatePaymentRules(paymentTypeName, paidAmount, totalAmount);

        Project project = new Project();
        mapRequestDtoToProject(project, requestDto);
        project.setProduct(product);
        project.setCompany(company);
        project.setContact(contact);
        project.setCreatedBy(createdBy.getId());
        project.setUpdatedBy(updatedBy.getId());
        project.setCreatedDate(new Date());
        project.setUpdatedDate(new Date());
        project.setDeleted(false);
        project.setSalesPersonId(requestDto.getSalesPersonId());
        project.setSalesPersonName(requestDto.getSalesPersonName());
        project.setActive(true);
        project.setUnit(unit);


        ProjectStatus openStatus = projectStatusRepository.findById(StatusConstants.PROJECT_OPEN_ID)
                .orElseThrow(() -> new ResourceNotFoundException("System status OPEN (ID=1) not found", "ERR_SYSTEM_STATUS_MISSING"));
        project.setStatus(openStatus);

        ProjectPaymentDetail paymentDetail = new ProjectPaymentDetail();
        paymentDetail.setProject(project);
        paymentDetail.setTotalAmount(totalAmount);
        paymentDetail.setDueAmount(dueAmount);
        paymentDetail.setPaymentType(paymentType);
        paymentDetail.setApprovedBy(approvedBy);
        paymentDetail.setCreatedBy(createdBy.getId());
        paymentDetail.setUpdatedBy(updatedBy.getId());
        paymentDetail.setCreatedDate(new Date());
        paymentDetail.setUpdatedDate(new Date());
        paymentDetail.setDate(LocalDate.now());
        paymentDetail.setDeleted(false);

        project.setPaymentDetail(paymentDetail);

        Map<String, Object> solutionDetails = leadFeignClient.getSolutionByIdOnly(product.getId());

        double professionalFee = extractProfessionalFee(solutionDetails);

        ProjectPriority priority = calculateProjectPrioritySafely(
                product.getId(),
                company.getRating(),
                totalAmount
        );

        project.setPriority(priority);

        logger.info(
                "Project priority calculated. companyId={}, rating={}, solutionId={}, professionalFee={}, totalAmount={}, priority={}",
                company.getId(),
                company.getRating(),
                product.getId(),
                professionalFee,
                totalAmount,
                priority
        );


        project = projectRepository.save(project);

        // =========================================================
        // PROJECT HISTORY - PROJECT CREATED
        // =========================================================
        saveProjectHistory(
                project,
                null,
                "PROJECT_CREATED",
                "PROJECT",
                project.getId(),
                "Project created",
                "Project " + project.getProjectNo() + " created successfully",
                null,
                null,
                project.getStatus() != null ? project.getStatus().getName() : null,
                createdBy.getId(),
                null,
                null,
                null
        );

        try {
            projectMailService.sendProjectCreatedMail(project, contact);
        } catch (Exception e) {
            logger.error("Failed to send project created mail to client contact: {}", contact.getEmail(), e);
        }

        if (paidAmount > 0) {
            ProjectPaymentTransaction transaction = new ProjectPaymentTransaction();
            transaction.setProject(project);
            transaction.setAmount(paidAmount);
            transaction.setTransactionDate(new Date());
            transaction.setCreatedBy(createdBy.getId());
            transaction.setCreatedDate(new Date());
            projectPaymentTransactionRepository.save(transaction);

            // =====================================================
            // PROJECT HISTORY - INITIAL PAYMENT
            // =====================================================
            saveProjectHistory(
                    project,
                    null,
                    "PAYMENT_ADDED",
                    "PAYMENT",
                    transaction.getId(),
                    "Payment received",
                    "Initial payment of " + paidAmount
                            + " received for project " + project.getProjectNo(),
                    null,
                    String.valueOf(totalAmount),
                    String.valueOf(dueAmount),
                    createdBy.getId(),
                    null,
                    null,
                    null
            );
        }

        MilestoneStatus newStatus = milestoneStatusRepository.findById(StatusConstants.MILESTONE_NEW_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone status NEW (ID=1) not found", "ERR_SYSTEM_STATUS_MISSING"));

        for (ProductMilestoneMap milestone : milestones) {
            ProjectMilestoneAssignment assignment = new ProjectMilestoneAssignment();
            assignment.setProject(project);
            assignment.setProductMilestoneMap(milestone);
            assignment.setMilestone(milestone.getMilestone());
            assignment.setStatus(newStatus);
            assignment.setCreatedBy(createdBy.getId());
            assignment.setUpdatedBy(updatedBy.getId());
            assignment.setCreatedDate(new Date());
            assignment.setUpdatedDate(new Date());
            assignment.setDate(LocalDate.now());
            assignment.setDeleted(false);
            projectMilestoneAssignmentRepository.save(assignment);
        }

        updateMilestoneVisibilities(project, createdBy.getId());
        return mapToResponseDto(project);
    }
    private ProjectPriority calculateProjectPrioritySafely(
            Long solutionId,
            String companyRating,
            double projectTotalAmount
    ) {
        try {
            Map<String, Object> solutionDetails = leadFeignClient.getSolutionByIdOnly(solutionId);

            double professionalFee = extractProfessionalFee(solutionDetails);

            return calculateProjectPriorityByCompanyRating(
                    companyRating,
                    professionalFee,
                    projectTotalAmount
            );

        } catch (FeignException ex) {
            logger.error(
                    "Lead Service API failed while calculating project priority. solutionId={}, status={}, message={}. Defaulting priority to STANDARD",
                    solutionId,
                    ex.status(),
                    ex.getMessage(),
                    ex
            );
            return ProjectPriority.STANDARD;

        } catch (Exception ex) {
            logger.error(
                    "Unexpected error while calculating project priority. solutionId={}, message={}. Defaulting priority to STANDARD",
                    solutionId,
                    ex.getMessage(),
                    ex
            );
            return ProjectPriority.STANDARD;
        }
    }


    private ProjectPriority calculateProjectPriorityByCompanyRating(
            String companyRating,
            double solutionProfessionalFee,
            double projectTotalAmount
    ) {
        if (solutionProfessionalFee <= 0) {
            throw new ValidationException(
                    "Professional fee is required to calculate project priority",
                    "ERR_PROFESSIONAL_FEE_REQUIRED"
            );
        }

        double amountPercentage = (projectTotalAmount / solutionProfessionalFee) * 100.0;

        String rating = companyRating != null
                ? companyRating.trim().toUpperCase()
                : "";

        switch (rating) {
            case "BRONZE":
                // Less than 130% = STANDARD
                // 130% to less than 150% = HIGH
                // 150% and above = CRITICAL
                if (amountPercentage < 130.0) {
                    return ProjectPriority.STANDARD;
                } else if (amountPercentage < 150.0) {
                    return ProjectPriority.HIGH;
                } else {
                    return ProjectPriority.CRITICAL;
                }

            case "SILVER":
                // Less than 120% = STANDARD
                // 120% to less than 140% = HIGH
                // 140% and above = CRITICAL
                if (amountPercentage < 120.0) {
                    return ProjectPriority.STANDARD;
                } else if (amountPercentage < 140.0) {
                    return ProjectPriority.HIGH;
                } else {
                    return ProjectPriority.CRITICAL;
                }

            case "GOLD":
                // Less than 110% = STANDARD
                // 110% to less than 130% = HIGH
                // 130% and above = CRITICAL
                if (amountPercentage < 110.0) {
                    return ProjectPriority.STANDARD;
                } else if (amountPercentage < 130.0) {
                    return ProjectPriority.HIGH;
                } else {
                    return ProjectPriority.CRITICAL;
                }

            default:
                return ProjectPriority.STANDARD;
        }
    }

    private double extractProfessionalFee(Map<String, Object> solutionMap) {
        if (solutionMap == null || solutionMap.isEmpty()) {
            throw new ValidationException(
                    "Solution details not found from Lead Service",
                    "ERR_SOLUTION_DETAILS_NOT_FOUND"
            );
        }

        Object feeValue = solutionMap.get("professionalFee");

        if (feeValue == null) {
            // fallback if Lead API sends price instead of professionalFee
            feeValue = solutionMap.get("price");
        }

        if (feeValue == null) {
            throw new ValidationException(
                    "Professional fee not found in Lead Service response",
                    "ERR_PROFESSIONAL_FEE_NOT_FOUND"
            );
        }

        if (feeValue instanceof Number) {
            return ((Number) feeValue).doubleValue();
        }

        try {
            return Double.parseDouble(feeValue.toString());
        } catch (NumberFormatException ex) {
            throw new ValidationException(
                    "Invalid professional fee value from Lead Service",
                    "ERR_INVALID_PROFESSIONAL_FEE"
            );
        }
    }
    private void validatePaymentRules(String paymentTypeName, double paidAmount, double totalAmount) {
        if ("FULL".equalsIgnoreCase(paymentTypeName) || "Full Payment".equalsIgnoreCase(paymentTypeName)) {
            if (paidAmount != totalAmount) {
                throw new ValidationException("FULL payment requires the entire amount", "ERR_INVALID_FULL_PAYMENT");
            }
        } else if ("PARTIAL".equalsIgnoreCase(paymentTypeName)) {
            double percentage = (paidAmount / totalAmount) * 100.0;
            if (Math.abs(percentage - 50.0) > 0.01) {
                throw new ValidationException("PARTIAL payment must be exactly 50%", "ERR_PARTIAL_MUST_BE_50_PERCENT");
            }
        } else if ("INSTALLMENT".equalsIgnoreCase(paymentTypeName)) {
            if (paidAmount > totalAmount) {
                throw new ValidationException("Payment cannot exceed total amount", "ERR_EXCEEDS_TOTAL");
            }
        } else if ("PURCHASE_ORDER".equalsIgnoreCase(paymentTypeName) || "Purchase Order Payment".equalsIgnoreCase(paymentTypeName)) {
            if (paidAmount > 0) {
                throw new ValidationException("No initial payment allowed for PO", "ERR_INVALID_PO_PAYMENT");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponseDto> getAllProjects(
            Long userId,
            int page,
            int size,
            List<String> statuses
    ) {
        logger.info(
                "[GET-PROJECTS-START] userId={} | page={} | size={} | statuses={}",
                userId, page, size, statuses
        );

        if (page < 0) {
            throw new ValidationException(
                    "Page number cannot be negative",
                    "ERR_INVALID_PAGE"
            );
        }

        if (size < 1) {
            throw new ValidationException(
                    "Page size must be greater than zero",
                    "ERR_INVALID_PAGE_SIZE"
            );
        }

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId,
                        "ERR_USER_NOT_FOUND"
                ));

        boolean isAdmin = hasRole(user, "ADMIN");
        boolean isOperationHead = hasRole(user, "OPERATION_HEAD");
        boolean fullAccess = isAdmin || isOperationHead;

        boolean managerAccess = user.isManagerFlag();

        List<Long> departmentIds = managerAccess
                ? user.getDepartments()
                .stream()
                .filter(Objects::nonNull)
                .map(Department::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList()
                : List.of();

        /*
         * Avoid an empty IN (:departmentIds) parameter.
         * -1 should never match a real department.
         */
        if (departmentIds.isEmpty()) {
            departmentIds = List.of(-1L);
        }

        List<String> normalizedStatuses;

        if (statuses == null || statuses.isEmpty()) {
            normalizedStatuses = List.of(
                    "OPEN",
                    "IN_PROGRESS",
                    "REOPENED"
            );
        } else {
            normalizedStatuses = statuses.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(status -> !status.isBlank())
                    .map(String::toUpperCase)
                    .distinct()
                    .toList();

            if (normalizedStatuses.isEmpty()) {
                normalizedStatuses = List.of(
                        "OPEN",
                        "IN_PROGRESS",
                        "REOPENED"
                );
            }
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        logger.info(
                "[GET-PROJECTS-ACCESS] userId={} | admin={} | operationHead={} " +
                        "| manager={} | departmentIds={}",
                userId,
                isAdmin,
                isOperationHead,
                managerAccess,
                departmentIds
        );

        Page<Project> projectPage =
                projectRepository.findAccessibleProjects(
                        userId,
                        fullAccess,
                        managerAccess,
                        departmentIds,
                        normalizedStatuses,
                        pageable
                );

        logger.info(
                "[GET-PROJECTS-SUCCESS] userId={} | projectsOnPage={} | totalProjects={}",
                userId,
                projectPage.getNumberOfElements(),
                projectPage.getTotalElements()
        );

        List<Project> projects = projectPage.getContent();

        List<ProjectResponseDto> responses = projects.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        enrichWithMilestoneUserDetails(projects, responses);

// NEW
        enrichWithMilestones(projects, responses);

        return responses;
    }

    private void enrichWithMilestones(
            List<Project> projects,
            List<ProjectResponseDto> responses
    ) {

        if (projects == null || projects.isEmpty()
                || responses == null || responses.isEmpty()) {
            return;
        }

        List<Long> projectIds = projects.stream()
                .filter(Objects::nonNull)
                .map(Project::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (projectIds.isEmpty()) {
            return;
        }

        /*
         * Single batch query for all projects on the current page.
         *
         * Avoid:
         * project 1 -> query milestones
         * project 2 -> query milestones
         * project 3 -> query milestones
         *
         * Instead:
         * all project IDs -> one query
         */
        List<ProjectMilestoneAssignment> assignments =
                projectMilestoneAssignmentRepository
                        .findDashboardAssignmentsByProjectIds(projectIds);

        Map<Long, List<ProjectMilestoneListDto>> milestonesByProject =
                assignments.stream()
                        .filter(Objects::nonNull)
                        .filter(assignment ->
                                assignment.getProject() != null
                                        && assignment.getProject().getId() != null
                        )
                        .collect(Collectors.groupingBy(
                                assignment -> assignment.getProject().getId(),
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        this::mapToProjectMilestoneListDto,
                                        Collectors.toList()
                                )
                        ));

        for (ProjectResponseDto response : responses) {

            if (response == null || response.getId() == null) {
                continue;
            }

            response.setMilestones(
                    milestonesByProject.getOrDefault(
                            response.getId(),
                            List.of()
                    )
            );
        }
    }

    private ProjectMilestoneListDto mapToProjectMilestoneListDto(
            ProjectMilestoneAssignment assignment
    ) {

        ProjectMilestoneListDto dto =
                new ProjectMilestoneListDto();

        if (assignment.getMilestone() != null) {

            dto.setId(
                    assignment.getMilestone().getId()
            );

            dto.setName(
                    assignment.getMilestone().getName()
            );
        }

        if (assignment.getStatus() != null) {

            dto.setStatus(
                    assignment.getStatus().getName()
            );
        }

        return dto;
    }

    /*
     * Attaches the last completed milestone and its assigned user to each
     * project on the page using a single batch query, avoiding N+1.
     *
     * "Last" is resolved by completedDate. Legacy rows where completedDate
     * was never stamped fall back to milestone order.
     */
    /*
     * Attaches two things to every project on the page, using batch
     * queries so the page cost stays flat regardless of page size:
     *
     * 1. Last completed milestone and the user who completed it.
     *    Resolved by completedDate, falling back to milestone order
     *    for legacy rows where completedDate was never stamped.
     *
     * 2. Current milestone and its assigned user. This is the
     *    lowest-order visible milestone that is not yet completed.
     */
    private void enrichWithMilestoneUserDetails(
            List<Project> projects,
            List<ProjectResponseDto> responses
    ) {
        if (projects == null || projects.isEmpty()) {
            return;
        }

        List<Long> projectIds = projects.stream()
                .filter(Objects::nonNull)
                .map(Project::getId)
                .filter(Objects::nonNull)
                .toList();

        if (projectIds.isEmpty()) {
            return;
        }

        // =========================================================
        // 1. LAST COMPLETED MILESTONE
        // =========================================================

        List<ProjectMilestoneAssignment> completedAssignments =
                projectMilestoneAssignmentRepository
                        .findCompletedAssignmentsByProjectIds(
                                projectIds,
                                StatusConstants.MILESTONE_COMPLETED_ID
                        );

        /*
         * Newest sorts last, so maxBy gives the latest completed
         * milestone. nullsFirst stops rows without completedDate
         * from beating rows that actually have one.
         */
        Comparator<ProjectMilestoneAssignment> latestLast =
                Comparator
                        .comparing(
                                ProjectMilestoneAssignment::getCompletedDate,
                                Comparator.nullsFirst(Comparator.naturalOrder())
                        )
                        .thenComparingInt(a ->
                                a.getProductMilestoneMap() != null
                                        ? a.getProductMilestoneMap().getOrder()
                                        : Integer.MIN_VALUE
                        )
                        .thenComparing(ProjectMilestoneAssignment::getId);

        Map<Long, ProjectMilestoneAssignment> lastCompletedByProject =
                completedAssignments == null
                        ? Map.of()
                        : completedAssignments.stream()
                        .filter(Objects::nonNull)
                        .filter(a -> a.getProject() != null
                                && a.getProject().getId() != null)
                        .collect(Collectors.toMap(
                                a -> a.getProject().getId(),
                                a -> a,
                                BinaryOperator.maxBy(latestLast)
                        ));

        // =========================================================
        // 2. CURRENT MILESTONE
        // =========================================================

        List<ProjectMilestoneAssignment> activeAssignments =
                projectMilestoneAssignmentRepository
                        .findActiveAssignmentsByProjectIds(
                                projectIds,
                                StatusConstants.MILESTONE_COMPLETED_ID
                        );

        Comparator<ProjectMilestoneAssignment> earliestFirst =
                Comparator
                        .comparingInt((ProjectMilestoneAssignment a) ->
                                a.getProductMilestoneMap() != null
                                        ? a.getProductMilestoneMap().getOrder()
                                        : Integer.MAX_VALUE
                        )
                        .thenComparing(ProjectMilestoneAssignment::getId);

        Map<Long, ProjectMilestoneAssignment> currentByProject =
                activeAssignments == null
                        ? Map.of()
                        : activeAssignments.stream()
                        .filter(Objects::nonNull)
                        .filter(a -> a.getProject() != null
                                && a.getProject().getId() != null)
                        .collect(Collectors.toMap(
                                a -> a.getProject().getId(),
                                a -> a,
                                BinaryOperator.minBy(earliestFirst)
                        ));

        logger.info(
                "[MILESTONE-ENRICHMENT] projects={} | withCompleted={} | withCurrent={}",
                projectIds.size(),
                lastCompletedByProject.size(),
                currentByProject.size()
        );

        // =========================================================
        // 3. MAP ONTO RESPONSE
        // =========================================================

        for (ProjectResponseDto dto : responses) {

            // ---- Current milestone ----

            ProjectMilestoneAssignment current =
                    currentByProject.get(dto.getId());

            if (current != null) {

                User currentUser = current.getAssignedUser();

                dto.setCurrentMilestoneAssignmentId(current.getId());

                dto.setCurrentMilestoneId(
                        current.getMilestone() != null
                                ? current.getMilestone().getId()
                                : null
                );

                dto.setCurrentMilestoneName(
                        getProjectMilestoneName(current)
                );

                dto.setCurrentMilestoneOrder(
                        current.getProductMilestoneMap() != null
                                ? current.getProductMilestoneMap().getOrder()
                                : null
                );

                dto.setCurrentMilestoneStatusName(
                        current.getStatus() != null
                                ? current.getStatus().getName()
                                : null
                );

                dto.setCurrentAssignedUserId(
                        currentUser != null ? currentUser.getId() : null
                );

                dto.setCurrentAssignedUserName(
                        currentUser != null ? currentUser.getFullName() : null
                );

                dto.setCurrentAssignedUserEmail(
                        currentUser != null ? currentUser.getEmail() : null
                );

                dto.setCurrentAssignedUserMobile(
                        currentUser != null ? currentUser.getContactNo() : null
                );

                logger.debug(
                        "[CURRENT-MILESTONE] projectId={} | assignmentId={} | milestone={} | status={} | assignedUserId={}",
                        dto.getId(),
                        current.getId(),
                        dto.getCurrentMilestoneName(),
                        dto.getCurrentMilestoneStatusName(),
                        currentUser != null ? currentUser.getId() : null
                );

            } else {
                logger.debug(
                        "[CURRENT-MILESTONE] No active visible milestone for projectId={}",
                        dto.getId()
                );
            }

            // ---- Last completed milestone ----

            ProjectMilestoneAssignment assignment =
                    lastCompletedByProject.get(dto.getId());

            if (assignment == null) {
                logger.debug(
                        "[LAST-COMPLETED-MILESTONE] No completed milestone for projectId={}",
                        dto.getId()
                );
                continue;
            }

            User assignedUser = assignment.getAssignedUser();

            dto.setLastCompletedMilestoneAssignmentId(assignment.getId());

            dto.setLastCompletedMilestoneId(
                    assignment.getMilestone() != null
                            ? assignment.getMilestone().getId()
                            : null
            );

            dto.setLastCompletedMilestoneName(
                    getProjectMilestoneName(assignment)
            );

            dto.setLastCompletedMilestoneOrder(
                    assignment.getProductMilestoneMap() != null
                            ? assignment.getProductMilestoneMap().getOrder()
                            : null
            );

            dto.setLastCompletedMilestoneCompletedDate(
                    assignment.getCompletedDate()
            );

            dto.setLastCompletedMilestoneUserId(
                    assignedUser != null ? assignedUser.getId() : null
            );

            dto.setLastCompletedMilestoneUserName(
                    assignedUser != null ? assignedUser.getFullName() : null
            );

            dto.setLastCompletedMilestoneUserEmail(
                    assignedUser != null ? assignedUser.getEmail() : null
            );

            dto.setLastCompletedMilestoneUserMobile(
                    assignedUser != null ? assignedUser.getContactNo() : null
            );

            logger.debug(
                    "[LAST-COMPLETED-MILESTONE] projectId={} | assignmentId={} | milestone={} | completedDate={} | assignedUserId={}",
                    dto.getId(),
                    assignment.getId(),
                    dto.getLastCompletedMilestoneName(),
                    assignment.getCompletedDate(),
                    assignedUser != null ? assignedUser.getId() : null
            );
        }
    }

    private boolean hasRole(User user, String requiredRole) {
        if (user.getRoles() == null) {
            return false;
        }

        return user.getRoles()
                .stream()
                .filter(Objects::nonNull)
                .map(role -> role.getName())
                .filter(Objects::nonNull)
                .anyMatch(roleName -> requiredRole.equalsIgnoreCase(roleName));
    }


    @Override
    public long getProjectCount(Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        if (isAdmin) {
            return projectRepository.countByIsDeletedFalse();
        } else {
            List<Long> userIds = new ArrayList<>(List.of(userId));
            if (user.isManagerFlag()) {
                List<User> subordinates = userRepository.findByManagerIdAndIsDeletedFalse(userId);
                userIds.addAll(subordinates.stream().map(User::getId).toList());
            }
            return projectRepository.countByAssignedUserIds(userIds);
        }
    }


    @Override
    public void deleteProject(Long id) {
        Project project = projectRepository.findActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));
        project.setDeleted(true);
        project.setUpdatedDate(new Date());
        project.getPaymentDetail().setDeleted(true);
        project.getPaymentDetail().setUpdatedDate(new Date());
        project.getMilestoneAssignments().forEach(a -> {
            a.setDeleted(true);
            a.setUpdatedDate(new Date());
        });
        projectRepository.save(project);

        // =========================================================
        // PROJECT HISTORY - PROJECT DELETED
        // =========================================================
        saveProjectHistory(
                project,
                null,
                "PROJECT_DELETED",
                "PROJECT",
                project.getId(),
                "Project deleted",
                "Project " + project.getProjectNo() + " was marked as deleted",
                null,
                "ACTIVE",
                "DELETED",
                null,
                null,
                null,
                null
        );
    }


    @Override
    public ProjectResponseDto addPaymentTransaction(Long projectId, ProjectPaymentTransactionDto dto) {
        validateTransactionDto(dto);

        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));

        validateProjectAllowsPayment(project);

        ProjectPaymentDetail paymentDetail = projectPaymentDetailRepository.findByProjectIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment detail not found", "ERR_PAYMENT_DETAIL_NOT_FOUND"));

        double amount = dto.getAmount();
        double dueAmount = paymentDetail.getDueAmount();
        String paymentTypeName = paymentDetail.getPaymentType().getName();

        if (amount <= 0) throw new ValidationException("Amount must be positive", "ERR_INVALID_PAYMENT_AMOUNT");
        if (amount > dueAmount) throw new ValidationException("Amount exceeds due", "ERR_EXCEEDS_DUE_AMOUNT");

        if ("Full Payment".equalsIgnoreCase(paymentTypeName)) {
            if (dueAmount > 0 && amount != dueAmount) {
                throw new ValidationException("Full payment requires full due amount", "ERR_INVALID_FULL_PAYMENT_AMOUNT");
            }
        } else if ("Purchase Order Payment".equalsIgnoreCase(paymentTypeName)) {
            boolean allNonCertCompleted = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(projectId).stream()
                    .filter(a -> !a.getMilestone().getName().equalsIgnoreCase("Certification"))
                    .allMatch(a -> StatusConstants.MILESTONE_COMPLETED_ID.equals(a.getStatus().getId()));
            if (!allNonCertCompleted) {
                throw new ValidationException("All non-certification milestones must be completed for PO payment", "ERR_PO_PAYMENT_MILESTONE_NOT_COMPLETED");
            }
        }

        paymentDetail.setDueAmount(dueAmount - amount);
        User createdBy = userRepository.findActiveUserById(dto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        ProjectPaymentTransaction transaction = new ProjectPaymentTransaction();
        transaction.setProject(project);
        transaction.setAmount(amount);
        transaction.setTransactionDate(dto.getPaymentDate());
        transaction.setCreatedBy(createdBy.getId());
        transaction.setCreatedDate(new Date());

        paymentDetail.setUpdatedBy(createdBy.getId());
        paymentDetail.setUpdatedDate(new Date());

        projectPaymentTransactionRepository.save(transaction);
        projectPaymentDetailRepository.save(paymentDetail);

        // =========================================================
        // PROJECT HISTORY - PAYMENT ADDED
        // =========================================================
        saveProjectHistory(
                project,
                null,
                "PAYMENT_ADDED",
                "PAYMENT",
                transaction.getId(),
                "Payment received",
                "Payment of " + amount
                        + " received for project " + project.getProjectNo(),
                null,
                String.valueOf(dueAmount),
                String.valueOf(paymentDetail.getDueAmount()),
                createdBy.getId(),
                null,
                null,
                null
        );

        updateMilestoneVisibilities(project, createdBy.getId());
        return mapToResponseDto(project);
    }


    @Override
    public MilestoneHistoryResponseDto getMilestoneHistory(Long projectId, Long milestoneId, Long requestingUserId) {
        logger.info("Fetching history for milestone ID: {} in project ID: {} by user ID: {}", milestoneId, projectId, requestingUserId);

        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));

        User requestingUser = userRepository.findActiveUserById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        ProjectMilestoneAssignment assignment = projectMilestoneAssignmentRepository
                .findByProjectIdAndMilestoneIdAndIsDeletedFalse(projectId, milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found in this project", "MILESTONE_NOT_FOUND"));

        // Authorization: Admin, OpHead, Assigned user, Manager of assigned user, or milestone is visible
        boolean isAdmin = requestingUser.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        boolean isOpHead = requestingUser.getRoles().stream().anyMatch(r -> "OPERATION_HEAD".equals(r.getName()));
        boolean isAssigned = assignment.getAssignedUser() != null && assignment.getAssignedUser().getId().equals(requestingUserId);

        boolean isManagerOfAssigned = false;
        if (assignment.getAssignedUser() != null) {
            List<User> subordinates = userRepository.findByManagerIdAndIsDeletedFalse(requestingUserId);
            isManagerOfAssigned = subordinates.stream()
                    .anyMatch(u -> u.getId().equals(assignment.getAssignedUser().getId()));
        }

        if (!isAdmin && !isOpHead && !isAssigned && !isManagerOfAssigned && !assignment.isVisible()) {
            throw new ValidationException("You are not authorized to view this milestone history", "UNAUTHORIZED_MILESTONE_HISTORY_ACCESS");
        }

        return mapToSingleMilestoneHistoryDto(assignment);
    }

    private MilestoneHistoryResponseDto mapToSingleMilestoneHistoryDto(ProjectMilestoneAssignment assignment) {
        MilestoneHistoryResponseDto dto = new MilestoneHistoryResponseDto();

        dto.setMilestoneAssignmentId(assignment.getId());
        dto.setMilestoneName(assignment.getMilestone().getName());
        dto.setOrder(assignment.getProductMilestoneMap().getOrder());
        dto.setCreatedDate(assignment.getCreatedDate());

        User createdByUser = userRepository.findActiveUserById(assignment.getCreatedBy()).orElse(null);
        dto.setCreatedBy(assignment.getCreatedBy());
        dto.setCreatedByName(createdByUser != null ? createdByUser.getFullName() : "Unknown");

        dto.setCurrentStatus(assignment.getStatus().getName());
        dto.setCurrentStatusReason(assignment.getStatusReason());
        dto.setVisibleDate(assignment.getVisibleDate());
        dto.setStartedDate(assignment.getStartedDate());
        dto.setCompletedDate(assignment.getCompletedDate());
        dto.setVisibilityReason(assignment.getVisibilityReason());
        dto.setVisible(assignment.isVisible());
        dto.setReworkAttempts(assignment.getReworkAttempts());

        if (assignment.getAssignedUser() != null) {
            dto.setCurrentAssignedUserId(assignment.getAssignedUser().getId());
            dto.setCurrentAssignedUserName(assignment.getAssignedUser().getFullName());
        }

        // Assignment History
        List<ProjectAssignmentHistory> assignmentHistories = projectAssignmentHistoryRepository
                .findByMilestoneAssignmentIdAndIsDeletedFalse(assignment.getId())
                .stream()
                .sorted(Comparator.comparing(ProjectAssignmentHistory::getCreatedDate))
                .toList();

        dto.setAssignmentEvents(assignmentHistories.stream()
                .map(this::mapToAssignmentEventDto)
                .toList());

        // Status Change History
        List<MilestoneStatusHistory> statusHistories = milestoneStatusHistoryRepository
                .findByMilestoneAssignmentIdAndIsDeletedFalse(assignment.getId())
                .stream()
                .sorted(Comparator.comparing(MilestoneStatusHistory::getChangeDate))
                .toList();

        List<StatusChangeEventDto> statusEvents = statusHistories.stream()
                .map(this::mapToStatusChangeEventDto)
                .toList();

        if (statusEvents.isEmpty()) {
            StatusChangeEventDto initial = new StatusChangeEventDto();
            initial.setDate(assignment.getCreatedDate());
            initial.setPreviousStatus(null);
            initial.setNewStatus(assignment.getStatus().getName());
            initial.setChangedBy(assignment.getCreatedBy());
            User initialUser = userRepository.findActiveUserById(assignment.getCreatedBy()).orElse(null);
            initial.setChangedByName(initialUser != null ? initialUser.getFullName() : "System");
            initial.setReason("Initial status on project creation");
            statusEvents = List.of(initial);
        }

        dto.setStatusChangeEvents(statusEvents);
        return dto;
    }

    @Override
    public void updateMilestoneVisibilities(
            Project project,
            Long updatedById
    ) {
        /*
         * Basic null protection.
         */
        if (project == null) {
            logger.warn(
                    "Skipping milestone visibility update because project is null"
            );
            return;
        }

        if (project.getStatus() == null) {
            logger.warn(
                    "Skipping milestone visibility update because project status is null. projectId={}",
                    project.getId()
            );
            return;
        }

        /*
         * Do not update milestone visibility for administratively
         * locked or terminal projects.
         *
         * REOPENED is intentionally not included because milestone
         * workflow must continue after reopening.
         */
        Long projectStatusId = project.getStatus().getId();

        boolean isForceClosed =
                StatusConstants.PROJECT_FORCE_CLOSED_ID.equals(
                        projectStatusId
                );

        boolean isCancelled =
                StatusConstants.PROJECT_CANCELLED_ID.equals(
                        projectStatusId
                ) || project.isCancelled();

        boolean isRefunded =
                StatusConstants.PROJECT_REFUNDED_ID.equals(
                        projectStatusId
                );

        if (isForceClosed || isCancelled || isRefunded) {
            logger.info(
                    "Skipping milestone visibility update because project is locked. projectId={}, statusId={}, statusName={}",
                    project.getId(),
                    projectStatusId,
                    project.getStatus().getName()
            );
            return;
        }

        /*
         * Payment details are required for visibility calculation.
         */
        if (project.getPaymentDetail() == null) {
            logger.warn(
                    "Skipping milestone visibility update because payment detail is missing. projectId={}",
                    project.getId()
            );
            return;
        }

        if (project.getPaymentDetail().getPaymentType() == null) {
            logger.warn(
                    "Skipping milestone visibility update because payment type is missing. projectId={}",
                    project.getId()
            );
            return;
        }

        double totalAmount =
                project.getPaymentDetail().getTotalAmount();

        double dueAmount =
                project.getPaymentDetail().getDueAmount();

        double paidAmount =
                totalAmount - dueAmount;

        double paidPercentage =
                totalAmount > 0
                        ? (paidAmount / totalAmount) * 100.0
                        : 0.0;

        String paymentTypeName =
                project.getPaymentDetail()
                        .getPaymentType()
                        .getName();

        List<ProjectMilestoneAssignment> assignments =
                projectMilestoneAssignmentRepository
                        .findByProjectIdAndIsDeletedFalse(
                                project.getId()
                        );

        if (assignments == null || assignments.isEmpty()) {
            logger.info(
                    "No milestone assignments found for visibility update. projectId={}",
                    project.getId()
            );
            return;
        }

        /*
         * Remove invalid assignments before sorting/calculation.
         */
        List<ProjectMilestoneAssignment> validAssignments =
                assignments.stream()
                        .filter(Objects::nonNull)
                        .filter(assignment ->
                                assignment.getProductMilestoneMap() != null
                        )
                        .filter(assignment ->
                                assignment.getProductMilestoneMap()
                                        .getMilestone() != null
                        )
                        .sorted(
                                Comparator.comparingInt(
                                        assignment ->
                                                assignment
                                                        .getProductMilestoneMap()
                                                        .getOrder()
                                )
                        )
                        .toList();

        if (validAssignments.isEmpty()) {
            logger.warn(
                    "No valid milestone assignments found for project. projectId={}",
                    project.getId()
            );
            return;
        }

        MilestoneStatus completedStatus =
                milestoneStatusRepository
                        .findById(
                                StatusConstants.MILESTONE_COMPLETED_ID
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "System milestone status COMPLETED is missing",
                                        "ERR_COMPLETED_MILESTONE_STATUS_MISSING"
                                )
                        );

        logger.info(
                "Updating milestone visibility. projectId={}, paymentType={}, totalAmount={}, paidAmount={}, dueAmount={}, paidPercentage={}",
                project.getId(),
                paymentTypeName,
                totalAmount,
                paidAmount,
                dueAmount,
                paidPercentage
        );

        boolean isPurchaseOrderPayment =
                "PURCHASE_ORDER".equalsIgnoreCase(
                        paymentTypeName
                )
                        || "Purchase Order Payment".equalsIgnoreCase(
                        paymentTypeName
                );

        if (isPurchaseOrderPayment) {
            /*
             * Purchase Order flow:
             *
             * All non-certification milestones remain visible.
             * Certification becomes visible only when:
             * 1. All previous milestones are completed.
             * 2. Full payment has been received.
             */
            for (ProjectMilestoneAssignment assignment :
                    validAssignments) {

                ProductMilestoneMap map =
                        assignment.getProductMilestoneMap();

                String milestoneName =
                        map.getMilestone().getName();

                int currentOrder =
                        map.getOrder();

                boolean isCertification =
                        milestoneName != null
                                && "Certification".equalsIgnoreCase(
                                milestoneName.trim()
                        );

                boolean allPriorCompleted =
                        validAssignments.stream()
                                .filter(previousAssignment ->
                                        previousAssignment
                                                .getProductMilestoneMap()
                                                .getOrder() < currentOrder
                                )
                                .allMatch(previousAssignment ->
                                        previousAssignment.getStatus() != null
                                                && completedStatus
                                                .getId()
                                                .equals(
                                                        previousAssignment
                                                                .getStatus()
                                                                .getId()
                                                )
                                );

                boolean fullPaymentReceived =
                        Math.abs(dueAmount) < 0.01;

                boolean isVisible;

                String visibilityReason;

                if (isCertification) {
                    isVisible =
                            allPriorCompleted
                                    && fullPaymentReceived;

                    if (isVisible) {
                        visibilityReason = null;
                    } else if (!allPriorCompleted) {
                        visibilityReason =
                                "Prior milestones incomplete";
                    } else {
                        visibilityReason =
                                "Full payment required";
                    }
                } else {
                    /*
                     * Existing PO behaviour:
                     * all non-certification milestones remain visible.
                     */
                    isVisible = true;
                    visibilityReason = null;
                }

                logger.debug(
                        "PO milestone visibility calculated. projectId={}, assignmentId={}, milestone={}, order={}, allPriorCompleted={}, fullPaymentReceived={}, visible={}, reason={}",
                        project.getId(),
                        assignment.getId(),
                        milestoneName,
                        currentOrder,
                        allPriorCompleted,
                        fullPaymentReceived,
                        isVisible,
                        visibilityReason
                );

                updateVisibilityAndAutoAssign(
                        assignment,
                        isVisible,
                        visibilityReason,
                        map,
                        project,
                        updatedById
                );
            }

            return;
        }

        /*
         * Full, Partial, and Installment payment flows.
         *
         * Visibility is based on:
         * 1. Previous milestones being completed.
         * 2. Required cumulative payment percentage.
         *
         * Certification additionally requires complete payment.
         */
        double cumulativePaymentPercentage = 0.0;

        for (ProjectMilestoneAssignment assignment :
                validAssignments) {

            ProductMilestoneMap map =
                    assignment.getProductMilestoneMap();

            String milestoneName =
                    map.getMilestone().getName();

            int currentOrder =
                    map.getOrder();

            double milestonePaymentPercentage =
                    map.getPaymentPercentage();

            cumulativePaymentPercentage +=
                    milestonePaymentPercentage;

            boolean allPreviousCompleted =
                    validAssignments.stream()
                            .filter(previousAssignment ->
                                    previousAssignment
                                            .getProductMilestoneMap()
                                            .getOrder() < currentOrder
                            )
                            .allMatch(previousAssignment ->
                                    previousAssignment.getStatus() != null
                                            && completedStatus
                                            .getId()
                                            .equals(
                                                    previousAssignment
                                                            .getStatus()
                                                            .getId()
                                            )
                            );

            boolean isCertification =
                    milestoneName != null
                            && "Certification".equalsIgnoreCase(
                            milestoneName.trim()
                    );

            boolean isVisible;
            String visibilityReason;

            if (isCertification) {
                boolean fullPaymentReceived =
                        Math.abs(dueAmount) < 0.01;

                isVisible =
                        allPreviousCompleted
                                && fullPaymentReceived;

                if (isVisible) {
                    visibilityReason = null;
                } else if (!allPreviousCompleted) {
                    visibilityReason =
                            "Prior milestones incomplete";
                } else {
                    visibilityReason =
                            "Full payment required";
                }
            } else {
                boolean sufficientPaymentReceived =
                        paidPercentage + 0.0001
                                >= cumulativePaymentPercentage;

                isVisible =
                        allPreviousCompleted
                                && sufficientPaymentReceived;

                if (isVisible) {
                    visibilityReason = null;
                } else if (!allPreviousCompleted) {
                    visibilityReason =
                            "Previous milestones incomplete";
                } else {
                    visibilityReason =
                            "Insufficient payment";
                }
            }

            logger.debug(
                    "Milestone visibility calculated. projectId={}, assignmentId={}, milestone={}, order={}, milestonePaymentPercentage={}, cumulativePaymentPercentage={}, paidPercentage={}, allPreviousCompleted={}, visible={}, reason={}",
                    project.getId(),
                    assignment.getId(),
                    milestoneName,
                    currentOrder,
                    milestonePaymentPercentage,
                    cumulativePaymentPercentage,
                    paidPercentage,
                    allPreviousCompleted,
                    isVisible,
                    visibilityReason
            );

            updateVisibilityAndAutoAssign(
                    assignment,
                    isVisible,
                    visibilityReason,
                    map,
                    project,
                    updatedById
            );
        }
    }

    private void updateVisibilityAndAutoAssign(
            ProjectMilestoneAssignment assignment,
            boolean isVisible,
            String reason,
            ProductMilestoneMap map,
            Project project,
            Long updatedById
    ) {

        // =========================================================
        // BASIC DETAILS
        // =========================================================

        Long projectId =
                project != null
                        ? project.getId()
                        : null;

        Long milestoneId =
                assignment != null
                        && assignment.getMilestone() != null
                        ? assignment.getMilestone().getId()
                        : null;

        String milestoneName =
                assignment != null
                        && assignment.getMilestone() != null
                        ? assignment.getMilestone().getName()
                        : null;

        String paymentTypeName =
                project != null
                        && project.getPaymentDetail() != null
                        && project.getPaymentDetail().getPaymentType() != null
                        ? project.getPaymentDetail()
                        .getPaymentType()
                        .getName()
                        : "N/A";

        double paidPercentage =
                project != null
                        && project.getPaymentDetail() != null
                        && project.getPaymentDetail().getTotalAmount() > 0
                        ? (
                        (
                                project.getPaymentDetail().getTotalAmount()
                                        - project.getPaymentDetail().getDueAmount()
                        )
                                / project.getPaymentDetail().getTotalAmount()
                ) * 100
                        : 0;

        logger.debug(
                "updateVisibilityAndAutoAssign started. "
                        + "projectId={}, milestoneId={}, milestoneName={}, "
                        + "currentVisible={}, requestedVisible={}, reason={}, "
                        + "paymentType={}, paidPercentage={}, updatedById={}",
                projectId,
                milestoneId,
                milestoneName,
                assignment != null && assignment.isVisible(),
                isVisible,
                reason,
                paymentTypeName,
                paidPercentage,
                updatedById
        );

        // =========================================================
        // NULL SAFETY
        // =========================================================

        if (assignment == null) {

            logger.warn(
                    "Skipping milestone visibility update because assignment is null. "
                            + "projectId={}",
                    projectId
            );

            return;
        }

        if (map == null) {

            logger.warn(
                    "Skipping milestone visibility update because "
                            + "ProductMilestoneMap is null. "
                            + "projectId={}, milestoneId={}",
                    projectId,
                    milestoneId
            );

            return;
        }

        // =========================================================
        // 1. VISIBILITY
        // =========================================================

        boolean visibilityChanged =
                assignment.isVisible() != isVisible
                        || !Objects.equals(
                        reason,
                        assignment.getVisibilityReason()
                );

        logger.debug(
                "Milestone visibility change evaluated. "
                        + "projectId={}, milestoneId={}, visibilityChanged={}",
                projectId,
                milestoneId,
                visibilityChanged
        );

        if (visibilityChanged) {

            boolean previousVisible = assignment.isVisible();
            String previousVisibilityReason = assignment.getVisibilityReason();

            logger.info(
                    "Updating milestone visibility. "
                            + "projectId={}, milestoneId={}, visible={}, reason={}",
                    projectId,
                    milestoneId,
                    isVisible,
                    reason
            );

            assignment.setVisible(
                    isVisible
            );

            assignment.setVisibilityReason(
                    reason
            );

            assignment.setVisibleDate(
                    isVisible
                            ? new Date()
                            : null
            );

            assignment.setUpdatedBy(
                    updatedById
            );

            assignment.setUpdatedDate(
                    new Date()
            );

            projectMilestoneAssignmentRepository.save(
                    assignment
            );

            // =====================================================
            // PROJECT HISTORY - MILESTONE VISIBILITY CHANGED
            // =====================================================
            saveProjectHistory(
                    project,
                    assignment,
                    "MILESTONE_VISIBILITY_CHANGED",
                    "MILESTONE",
                    assignment.getId(),
                    isVisible ? "Milestone became visible" : "Milestone became hidden",
                    "Milestone " + milestoneName
                            + " visibility changed from "
                            + previousVisible + " to " + isVisible,
                    reason != null ? reason : previousVisibilityReason,
                    String.valueOf(previousVisible),
                    String.valueOf(isVisible),
                    updatedById,
                    null,
                    null,
                    null
            );

            logger.info(
                    "Milestone visibility updated successfully. "
                            + "projectId={}, milestoneId={}, visible={}",
                    projectId,
                    milestoneId,
                    isVisible
            );

        } else {

            logger.debug(
                    "Milestone visibility update skipped because "
                            + "no change was detected. "
                            + "projectId={}, milestoneId={}",
                    projectId,
                    milestoneId
            );
        }

        // =========================================================
        // 2. AUTO ASSIGNMENT
        // =========================================================

        boolean shouldAutoAssign =
                isVisible
                        && !map.isAutoGenerated()
                        && assignment.getAssignedUser() == null;

        logger.debug(
                "Auto-assignment evaluated. "
                        + "projectId={}, milestoneId={}, shouldAutoAssign={}, "
                        + "visible={}, autoGenerated={}, alreadyAssigned={}",
                projectId,
                milestoneId,
                shouldAutoAssign,
                isVisible,
                map.isAutoGenerated(),
                assignment.getAssignedUser() != null
        );

        if (shouldAutoAssign) {

            logger.info(
                    "Triggering milestone auto-assignment. "
                            + "projectId={}, milestoneId={}, milestoneName={}",
                    projectId,
                    milestoneId,
                    milestoneName
            );

            AssignmentResult result =
                    autoAssignmentService.assignMilestoneUser(
                            map,
                            project,
                            updatedById
                    );

            assignment.setAssignedUser(
                    result != null
                            ? result.getUser()
                            : null
            );

            assignment.setStatusReason(
                    result != null
                            ? result.getReason()
                            : "Auto-assign failed"
            );

            assignment.setUpdatedBy(
                    updatedById
            );

            assignment.setUpdatedDate(
                    new Date()
            );

            projectMilestoneAssignmentRepository.save(
                    assignment
            );

            logger.info(
                    "Milestone auto-assignment completed. "
                            + "projectId={}, milestoneId={}, "
                            + "assignedUserId={}, assignedUserName={}, reason={}",
                    projectId,
                    milestoneId,
                    result != null
                            && result.getUser() != null
                            ? result.getUser().getId()
                            : null,
                    result != null
                            && result.getUser() != null
                            ? result.getUser().getFullName()
                            : null,
                    result != null
                            ? result.getReason()
                            : "Auto-assign failed"
            );

            // =====================================================
            // ASSIGNMENT HISTORY
            // =====================================================

            ProjectAssignmentHistory history =
                    new ProjectAssignmentHistory();

            history.setProject(
                    project
            );

            history.setMilestoneAssignment(
                    assignment
            );

            history.setAssignedUser(
                    result != null
                            ? result.getUser()
                            : null
            );

            history.setAssignmentReason(
                    result != null
                            ? result.getReason()
                            : "Auto-assign failed"
            );

            history.setCreatedDate(
                    new Date()
            );

            history.setUpdatedDate(
                    new Date()
            );

            history.setCreatedBy(
                    updatedById
            );

            history.setUpdatedBy(
                    updatedById
            );

            history.setDeleted(
                    false
            );

            projectAssignmentHistoryRepository.save(
                    history
            );

            logger.debug(
                    "Project assignment history saved. "
                            + "projectId={}, milestoneId={}, assignedUserId={}",
                    projectId,
                    milestoneId,
                    result != null
                            && result.getUser() != null
                            ? result.getUser().getId()
                            : null
            );

            // =====================================================
            // PROJECT HISTORY - MILESTONE ASSIGNED
            // =====================================================
            if (result != null && result.getUser() != null) {
                saveProjectHistory(
                        project,
                        assignment,
                        "MILESTONE_ASSIGNED",
                        "MILESTONE",
                        assignment.getId(),
                        "Milestone assigned",
                        "Milestone " + milestoneName
                                + " assigned to " + result.getUser().getFullName(),
                        result.getReason(),
                        null,
                        result.getUser().getFullName(),
                        updatedById,
                        null,
                        null,
                        result.getUser()
                );
            }
        }

        // =========================================================
        // 3. PROCUREMENT MILESTONE HANDLING
        // =========================================================

        boolean isProcurement =
                milestoneName != null
                        && "Procurement".equalsIgnoreCase(
                        milestoneName.trim()
                );

        logger.info(
                "Procurement milestone check. "
                        + "projectId={}, milestoneId={}, milestoneName={}, "
                        + "isProcurement={}, visible={}",
                projectId,
                milestoneId,
                milestoneName,
                isProcurement,
                isVisible
        );

        /*
         * IMPORTANT:
         *
         * ProcurementMilestoneAssignment is a backend workflow record.
         *
         * It MUST exist whenever the project contains a Procurement
         * milestone.
         *
         * It must NOT depend on milestone visibility.
         *
         * Visibility only controls whether the milestone is currently
         * accessible/displayed to the user.
         */
        if (isProcurement) {

            logger.info(
                    "Ensuring ProcurementMilestoneAssignment exists. "
                            + "projectId={}, milestoneId={}, visible={}",
                    projectId,
                    milestoneId,
                    isVisible
            );

            handleProcurementMilestoneCreation(
                    assignment,
                    updatedById
            );

            logger.info(
                    "ProcurementMilestoneAssignment ensured successfully. "
                            + "projectId={}, milestoneId={}",
                    projectId,
                    milestoneId
            );
        }

        // =========================================================
        // COMPLETE
        // =========================================================

        logger.debug(
                "updateVisibilityAndAutoAssign completed. "
                        + "projectId={}, milestoneId={}",
                projectId,
                milestoneId
        );
    }


    /**
     * Creates ProcurementMilestoneAssignment when Procurement milestone becomes visible.
     *
     * Flow:
     * 1. If milestone is not Procurement -> do nothing.
     * 2. If procurement assignment already exists -> do nothing.
     * 3. Create procurement assignment.
     * 4. Fetch vendors by project product/service.
     * 5. If vendors exist -> status VENDOR_SHORTLISTED.
     * 6. If vendors do not exist -> status VENDOR_REQUIRED.
     */
    private void handleProcurementMilestoneCreation(ProjectMilestoneAssignment generalAssignment, Long userId) {

        if (generalAssignment == null
                || generalAssignment.getMilestone() == null
                || generalAssignment.getProject() == null) {
            return;
        }

        if (!"Procurement".equalsIgnoreCase(generalAssignment.getMilestone().getName())) {
            return;
        }

        Long projectId = generalAssignment.getProject().getId();
        Long milestoneId = generalAssignment.getMilestone().getId();

        // Avoid duplicate procurement assignment
        boolean alreadyExists = procurementMilestoneAssignmentRepository
                .existsByProjectIdAndMilestoneId(projectId, milestoneId);

        if (alreadyExists) {
            return;
        }

        ProcurementMilestoneAssignment procurement = new ProcurementMilestoneAssignment();

        procurement.setProject(generalAssignment.getProject());
        procurement.setMilestone(generalAssignment.getMilestone());
        procurement.setProductMilestoneMap(generalAssignment.getProductMilestoneMap());
        procurement.setAssignedTo(generalAssignment.getAssignedUser());

        procurement.setCreatedBy(userId);
        procurement.setUpdatedBy(userId);
        procurement.setCreatedDate(new Date());
        procurement.setUpdatedDate(new Date());
        procurement.setDeleted(false);

        Long productId = generalAssignment.getProject().getProduct() != null
                ? generalAssignment.getProject().getProduct().getId()
                : null;

        if (productId == null) {
            procurement.setStatus(ProcurementStatus.VENDOR_REQUIRED);

            procurementMilestoneAssignmentRepository.save(procurement);

            logger.warn(
                    "ProcurementMilestoneAssignment created with VENDOR_REQUIRED because project has no product. projectId={}",
                    projectId
            );

            return;
        }


        List<Vendor> vendors = vendorRepository.findAllByStatusAndIsDeletedFalse(
                VendorStatus.ACTIVE, false);


        if (vendors == null || vendors.isEmpty()) {
            procurement.setStatus(ProcurementStatus.VENDOR_REQUIRED);

            logger.warn(
                    "No vendor available for projectId={}, productId={}. Please create vendor and map it with this service.",
                    projectId,
                    productId
            );
        } else {
            procurement.setStatus(ProcurementStatus.VENDOR_SHORTLISTED);
            procurement.setVendorShortlistedDate(new Date());

            logger.info(
                    "Vendors available for projectId={}, productId={}. Eligible vendor count={}",
                    projectId,
                    productId,
                    vendors.size()
            );
        }

        procurementMilestoneAssignmentRepository.save(procurement);

        logger.info(
                "ProcurementMilestoneAssignment auto-created for projectId={}, milestone={}, status={}",
                projectId,
                generalAssignment.getMilestone().getName(),
                procurement.getStatus()
        );
    }

    @Cacheable(value = "milestoneMaps", key = "#productId")
    public List<ProductMilestoneMap> getMilestoneMaps(Long productId) {
        return productMilestoneMapRepository
                .findByProductId(
                        productId,
                        PageRequest.of(
                                0,
                                Integer.MAX_VALUE,
                                Sort.by(Sort.Direction.ASC, "order")
                        )
                )
                .getContent();
    }

    private void validateTransactionDto(ProjectPaymentTransactionDto dto) {
        if (dto.getAmount() == null || dto.getAmount() <= 0) throw new ValidationException("Invalid amount", "ERR_INVALID_PAYMENT_AMOUNT");
        if (dto.getPaymentDate() == null) throw new ValidationException("Payment date required", "ERR_NULL_PAYMENT_DATE");
        if (dto.getCreatedBy() == null) throw new ValidationException("CreatedBy required", "ERR_NULL_CREATED_BY");
    }

    @Override
    @Transactional
    public void setApplicantType(Long projectId, Long applicantTypeId) {
        if (projectId == null || applicantTypeId == null) {
            throw new ValidationException("Project ID and Applicant Type ID are required", "ERR_NULL_IDS");
        }

        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or has been deleted", "ERR_PROJECT_NOT_FOUND"));

        ApplicantType applicantType = applicantTypeRepository
                .findByIdAndIsActiveTrueAndIsDeletedFalse(applicantTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant Type not found or is inactive/deleted", "ERR_APPLICANT_TYPE_NOT_FOUND"));

        String previousApplicantType = project.getApplicantType() != null
                ? project.getApplicantType().getName()
                : null;

        project.setApplicantType(applicantType);
        project.setUpdatedDate(new Date());

        projectRepository.save(project);

        // =========================================================
        // PROJECT HISTORY - APPLICANT TYPE CHANGED
        // =========================================================
        saveProjectHistory(
                project,
                null,
                "APPLICANT_TYPE_CHANGED",
                "PROJECT",
                project.getId(),
                "Applicant type changed",
                "Applicant type changed from "
                        + (previousApplicantType != null ? previousApplicantType : "Not Set")
                        + " to " + applicantType.getName(),
                null,
                previousApplicantType,
                applicantType.getName(),
                null,
                null,
                null,
                null
        );

        logger.info("Applicant Type successfully set to '{}' (ID: {}) for project ID: {}",
                applicantType.getName(), applicantType.getId(), projectId);
    }

    private void mapRequestDtoToProject(Project project, ProjectRequestDto dto) {
        project.setName(dto.getName().trim());
        project.setProjectNo(dto.getProjectNo().trim());
        project.setLeadId(dto.getLeadId());
        project.setDate(dto.getDate());
        project.setUnbilledNumber(dto.getUnbilledNumber());
        project.setEstimateNumber(dto.getEstimateNumber());
    }

    private ProjectResponseDto mapToResponseDto(Project project) {
        ProjectResponseDto dto = new ProjectResponseDto();

        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setProjectNo(project.getProjectNo());

        dto.setProductId(project.getProduct() != null ? project.getProduct().getId() : null);

        dto.setCompanyId(project.getCompany() != null ? project.getCompany().getId() : null);
        dto.setCompanyName(project.getCompany() != null ? project.getCompany().getName() : null);

        dto.setContactId(project.getContact() != null ? project.getContact().getId() : null);
        dto.setContactName(project.getContact() != null ? project.getContact().getName() : null);

        if (project.getUnit() != null) {
            dto.setUnitId(project.getUnit().getId());
            dto.setUnitName(project.getUnit().getUnitName());
        }

        dto.setLeadId(project.getLeadId());
        dto.setDate(project.getDate());

        dto.setTotalAmount(
                project.getPaymentDetail() != null
                        ? project.getPaymentDetail().getTotalAmount()
                        : 0.0
        );

        dto.setDueAmount(
                project.getPaymentDetail() != null
                        ? project.getPaymentDetail().getDueAmount()
                        : 0.0
        );

        dto.setPaymentTypeId(
                project.getPaymentDetail() != null
                        && project.getPaymentDetail().getPaymentType() != null
                        ? project.getPaymentDetail().getPaymentType().getId()
                        : null
        );

        dto.setApprovedById(
                project.getPaymentDetail() != null
                        && project.getPaymentDetail().getApprovedBy() != null
                        ? project.getPaymentDetail().getApprovedBy().getId()
                        : null
        );

        dto.setCreatedDate(project.getCreatedDate());
        dto.setUpdatedDate(project.getUpdatedDate());
        dto.setDeleted(project.isDeleted());
        dto.setActive(project.isActive());

        dto.setUnbilledNumber(project.getUnbilledNumber());
        dto.setEstimateNumber(project.getEstimateNumber());

        dto.setPriority(project.getPriority() != null ? project.getPriority().name() : null);

        dto.setSalesPersonId(project.getSalesPersonId());
        dto.setSalesPersonName(project.getSalesPersonName());

        dto.setStatusId(project.getStatus() != null ? project.getStatus().getId() : null);
        dto.setStatusName(project.getStatus() != null ? project.getStatus().getName() : null);

        /*
         * PO Billing Eligibility:
         *
         * For Purchase Order projects, tax invoice can be raised only after
         * all non-Certification milestones are completed.
         */
        boolean poBillingEligible = false;

        String paymentTypeName = project.getPaymentDetail() != null
                && project.getPaymentDetail().getPaymentType() != null
                ? project.getPaymentDetail().getPaymentType().getName()
                : null;

        boolean isPurchaseOrderPayment =
                "PURCHASE_ORDER".equalsIgnoreCase(paymentTypeName)
                        || "Purchase Order Payment".equalsIgnoreCase(paymentTypeName);

        if (isPurchaseOrderPayment) {
            List<ProjectMilestoneAssignment> assignments =
                    projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());

            poBillingEligible = assignments.stream()
                    .filter(a -> a.getMilestone() != null)
                    .filter(a -> !"Certification".equalsIgnoreCase(a.getMilestone().getName()))
                    .allMatch(a ->
                            a.getStatus() != null
                                    && "COMPLETED".equalsIgnoreCase(a.getStatus().getName())
                    );
        }

        dto.setPoBillingEligible(poBillingEligible);

        long totalMilestones =
                projectMilestoneAssignmentRepository
                        .countByProject_IdAndIsDeletedFalse(project.getId());

        long completedMilestones =
                projectMilestoneAssignmentRepository
                        .countByProject_IdAndStatus_NameAndIsDeletedFalse(project.getId(), "COMPLETED");

        int percentage = 0;

        if (totalMilestones > 0) {
            percentage = (int) ((completedMilestones * 100) / totalMilestones);
        }

        dto.setMilestoneCompletionPercentage(percentage);

        return dto;
    }

    @Override
    public Page<AssignedProjectResponseDto> getAssignedProjects(Long userId, int page, int size) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        PageRequest pageable = PageRequest.of(page, size * 10);
        Page<ProjectMilestoneAssignment> assignmentPage;

        MilestoneStatus newStatus = milestoneStatusRepository.findById(StatusConstants.MILESTONE_NEW_ID).orElse(null);
        MilestoneStatus inProgressStatus = milestoneStatusRepository.findById(StatusConstants.MILESTONE_IN_PROGRESS_ID).orElse(null);
        List<MilestoneStatus> activeStatuses = Arrays.asList(newStatus, inProgressStatus);

        if (newStatus == null || inProgressStatus == null) {
            throw new IllegalStateException("Critical milestone statuses (NEW/IN_PROGRESS) not found in DB");
        }

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        boolean isOpHead = user.getRoles().stream().anyMatch(r -> "OPERATION_HEAD".equals(r.getName()));

        if (isAdmin || isOpHead) {
            assignmentPage = projectMilestoneAssignmentRepository.findAllByIsDeletedFalse(pageable);
        } else if (user.isManagerFlag()) {
            List<Department> depts = user.getDepartments();
            if (depts.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
            List<Long> deptIds = depts.stream().map(Department::getId).toList();
            List<User> deptUsers = userRepository.findByDepartmentIdsIn(deptIds);
            List<Long> userIds = deptUsers.stream().map(User::getId).toList();
            if (!userIds.contains(userId)) userIds = new ArrayList<>(userIds);
            userIds.add(userId);
            assignmentPage = projectMilestoneAssignmentRepository.findByAssignedUserIdInAndIsVisibleTrueAndStatusIn(userIds, activeStatuses, pageable);
        } else {
            assignmentPage = projectMilestoneAssignmentRepository.findByAssignedUserIdAndIsVisibleTrueAndStatusIn(userId, activeStatuses, pageable);
        }

        List<ProjectMilestoneAssignment> assignments = assignmentPage.getContent();

        Map<Project, List<ProjectMilestoneAssignment>> grouped = assignments.stream()
                .collect(Collectors.groupingBy(ProjectMilestoneAssignment::getProject));

        List<AssignedProjectResponseDto> dtos = grouped.entrySet().stream()
                .map(e -> {
                    AssignedProjectResponseDto dto = new AssignedProjectResponseDto();
                    dto.setProject(mapToProjectDetailsDto(e.getKey(), userId));
                    return dto;
                }).toList();

        int start = Math.min(page * size, dtos.size());
        int end = Math.min(start + size, dtos.size());
        return new PageImpl<>(dtos.subList(start, end), PageRequest.of(page, size), dtos.size());
    }


    @Override
    public ProjectMilestoneResponseDto getProjectMilestones(
            Long projectId,
            Long userId
    ) {
        logger.info(
                "[GET-PROJECT-MILESTONES-START] projectId={} | userId={}",
                projectId,
                userId
        );

        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> {
                    logger.warn(
                            "[GET-PROJECT-MILESTONES-PROJECT-NOT-FOUND] projectId={}",
                            projectId
                    );

                    return new ResourceNotFoundException(
                            "Project not found with ID: " + projectId,
                            "ERR_PROJECT_NOT_FOUND"
                    );
                });

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> {
                    logger.warn(
                            "[GET-PROJECT-MILESTONES-USER-NOT-FOUND] userId={}",
                            userId
                    );

                    return new ResourceNotFoundException(
                            "User not found with ID: " + userId,
                            "ERR_USER_NOT_FOUND"
                    );
                });

        boolean isAdmin = hasRole(user, "ADMIN");
        boolean isOperationHead = hasRole(user, "OPERATION_HEAD");
        boolean isManager = user.isManagerFlag();

        logger.info(
                "[GET-PROJECT-MILESTONES-ROLE] projectId={} | userId={} | " +
                        "admin={} | operationHead={} | manager={}",
                projectId,
                userId,
                isAdmin,
                isOperationHead,
                isManager
        );

        /*
         * Recalculate milestone visibility before checking access.
         */
        updateMilestoneVisibilities(project, userId);

        List<ProjectMilestoneAssignment> allProjectAssignments =
                projectMilestoneAssignmentRepository
                        .findByProjectIdAndIsDeletedFalse(projectId);

        /*
         * ADMIN and OPERATION_HEAD can see every milestone.
         */
        if (isAdmin || isOperationHead) {
            List<ProjectMilestoneAssignment> sortedAssignments =
                    sortMilestoneAssignments(allProjectAssignments);

            logger.info(
                    "[GET-PROJECT-MILESTONES-FULL-ACCESS] projectId={} | " +
                            "userId={} | milestoneCount={}",
                    projectId,
                    userId,
                    sortedAssignments.size()
            );

            return buildProjectMilestoneResponse(
                    project,
                    userId,
                    sortedAssignments
            );
        }

        /*
         * Check whether the logged-in user is directly assigned to any
         * milestone in this project.
         *
         * Using the already fetched list avoids Optional returning multiple
         * results when the same user has more than one project milestone.
         */
        boolean isAssignedToProject = allProjectAssignments.stream()
                .filter(Objects::nonNull)
                .map(ProjectMilestoneAssignment::getAssignedUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .anyMatch(userId::equals);

        /*
         * Department-based manager access.
         *
         * Example:
         * - First CRT milestone is visible.
         * - assignedUser is null.
         * - CRT manager belongs to the CRT department.
         *
         * The CRT manager will still receive the milestone.
         */
        List<Long> managerDepartmentIds = new ArrayList<>();

        if (isManager && user.getDepartments() != null) {
            managerDepartmentIds = user.getDepartments()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(Department::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }

        List<ProjectMilestoneAssignment> departmentMilestones =
                new ArrayList<>();

        if (isManager && !managerDepartmentIds.isEmpty()) {
            departmentMilestones = projectMilestoneAssignmentRepository
                    .findVisibleMilestonesByProjectAndDepartments(
                            projectId,
                            managerDepartmentIds
                    );
        }

        boolean hasDepartmentManagerAccess =
                isManager && !departmentMilestones.isEmpty();

        logger.info(
                "[GET-PROJECT-MILESTONES-DEPARTMENT-CHECK] projectId={} | " +
                        "userId={} | manager={} | departmentIds={} | " +
                        "matchingVisibleMilestones={}",
                projectId,
                userId,
                isManager,
                managerDepartmentIds,
                departmentMilestones.size()
        );

        /*
         * Preserve existing direct-manager access.
         */
        List<User> subordinates =
                userRepository.findByManagerIdAndIsDeletedFalse(userId);

        List<Long> managedUserIds = subordinates.stream()
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        boolean isManagerOfAssignedUser = allProjectAssignments.stream()
                .filter(Objects::nonNull)
                .map(ProjectMilestoneAssignment::getAssignedUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .anyMatch(managedUserIds::contains);

        logger.info(
                "[GET-PROJECT-MILESTONES-ACCESS-CHECK] projectId={} | " +
                        "userId={} | directlyAssigned={} | " +
                        "departmentManagerAccess={} | directReportAccess={}",
                projectId,
                userId,
                isAssignedToProject,
                hasDepartmentManagerAccess,
                isManagerOfAssignedUser
        );

        if (!isAssignedToProject
                && !hasDepartmentManagerAccess
                && !isManagerOfAssignedUser) {

            logger.warn(
                    "[GET-PROJECT-MILESTONES-ACCESS-DENIED] projectId={} | userId={}",
                    projectId,
                    userId
            );

            throw new ValidationException(
                    "You are not authorized to view this project",
                    "ERR_UNAUTHORIZED_ACCESS"
            );
        }

        List<ProjectMilestoneAssignment> visibleAssignments;

        /*
         * Department manager receives all visible milestones mapped to
         * their department, including unassigned milestones.
         */
        if (hasDepartmentManagerAccess) {
            visibleAssignments = departmentMilestones;

            logger.info(
                    "[GET-PROJECT-MILESTONES-DEPARTMENT-MANAGER] projectId={} | " +
                            "managerId={} | milestoneCount={}",
                    projectId,
                    userId,
                    visibleAssignments.size()
            );
        } else if (isManagerOfAssignedUser) {
            /*
             * Preserve old manager/subordinate behavior.
             */
            List<Long> teamIds = new ArrayList<>(managedUserIds);

            if (!teamIds.contains(userId)) {
                teamIds.add(userId);
            }

            visibleAssignments = projectMilestoneAssignmentRepository
                    .findByProjectIdAndAssignedUserIdInAndIsVisibleTrue(
                            projectId,
                            teamIds
                    );

            logger.info(
                    "[GET-PROJECT-MILESTONES-DIRECT-MANAGER] projectId={} | " +
                            "managerId={} | teamIds={} | milestoneCount={}",
                    projectId,
                    userId,
                    teamIds,
                    visibleAssignments.size()
            );
        } else {
            /*
             * Regular user receives only visible milestones assigned to them.
             */
            visibleAssignments = projectMilestoneAssignmentRepository
                    .findByProjectIdAndAssignedUserIdAndIsVisibleTrueAndIsDeletedFalse(
                            projectId,
                            userId
                    );

            logger.info(
                    "[GET-PROJECT-MILESTONES-ASSIGNED-USER] projectId={} | " +
                            "userId={} | milestoneCount={}",
                    projectId,
                    userId,
                    visibleAssignments.size()
            );
        }

        visibleAssignments = sortMilestoneAssignments(visibleAssignments);

        if (visibleAssignments.isEmpty()) {
            logger.warn(
                    "[GET-PROJECT-MILESTONES-NO-VISIBLE-MILESTONE] " +
                            "projectId={} | userId={}",
                    projectId,
                    userId
            );

            throw new ValidationException(
                    "This project currently has no accessible visible milestone",
                    "ERR_PROJECT_NO_ACCESSIBLE_MILESTONE"
            );
        }

        visibleAssignments.forEach(assignment -> logger.debug(
                "[GET-PROJECT-MILESTONE] projectId={} | userId={} | " +
                        "assignmentId={} | milestone={} | visible={} | " +
                        "status={} | assignedUserId={}",
                projectId,
                userId,
                assignment.getId(),
                assignment.getMilestone() != null
                        ? assignment.getMilestone().getName()
                        : null,
                assignment.isVisible(),
                assignment.getStatus() != null
                        ? assignment.getStatus().getName()
                        : null,
                assignment.getAssignedUser() != null
                        ? assignment.getAssignedUser().getId()
                        : null
        ));

        logger.info(
                "[GET-PROJECT-MILESTONES-SUCCESS] projectId={} | " +
                        "userId={} | milestoneCount={}",
                projectId,
                userId,
                visibleAssignments.size()
        );

        return buildProjectMilestoneResponse(
                project,
                userId,
                visibleAssignments
        );
    }

    private ProjectMilestoneResponseDto buildProjectMilestoneResponse(
            Project project,
            Long userId,
            List<ProjectMilestoneAssignment> assignments
    ) {
        ProjectMilestoneResponseDto response =
                new ProjectMilestoneResponseDto();

        response.setProjectDetails(
                mapToProjectDetailsDto(project, userId)
        );

        response.setMilestones(
                assignments.stream()
                        .map(this::mapToAssignedMilestoneDto)
                        .collect(Collectors.toList())
        );

        return response;
    }


    private List<ProjectMilestoneAssignment> sortMilestoneAssignments(
            List<ProjectMilestoneAssignment> assignments
    ) {
        if (assignments == null) {
            return new ArrayList<>();
        }

        return assignments.stream()
                .sorted(
                        Comparator
                                .comparingInt((ProjectMilestoneAssignment a) ->
                                        a.getProductMilestoneMap() != null
                                                ? a.getProductMilestoneMap().getOrder()
                                                : Integer.MAX_VALUE
                                )
                                .thenComparing(ProjectMilestoneAssignment::getId)
                )
                .collect(Collectors.toList());
    }

    private ProjectDetailsDto mapToProjectDetailsDto(
            Project project,
            Long userId
    ) {
        logger.debug(
                "[MAP-PROJECT-DETAILS-START] projectId={} | requestingUserId={}",
                project.getId(),
                userId
        );

        ProjectDetailsDto dto = new ProjectDetailsDto();

        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setProjectNo(project.getProjectNo());

        dto.setPriority(
                project.getPriority() != null
                        ? project.getPriority().name()
                        : null
        );

        dto.setDate(project.getDate());

        if (project.getProduct() != null) {
            dto.setProductId(project.getProduct().getId());
            dto.setProductName(project.getProduct().getProductName());
        }

        if (project.getCompany() != null) {
            dto.setCompanyId(project.getCompany().getId());
            dto.setCompanyName(project.getCompany().getName());
            dto.setRating(project.getCompany().getRating());
        }

        if (project.getUnit() != null) {
            dto.setCompanyUnitId(project.getUnit().getId());
            dto.setCompanyUnitName(project.getUnit().getUnitName());
        }

        dto.setSalesPersonId(project.getSalesPersonId());
        dto.setSalesPersonName(project.getSalesPersonName());

        dto.setCreatedDate(project.getCreatedDate());
        dto.setUpdatedDate(project.getUpdatedDate());

        if (project.getApplicantType() != null) {
            dto.setApplicantId(project.getApplicantType().getId());
            dto.setApplicantName(project.getApplicantType().getName());
        }

        // =========================================================
        // PAYMENT DETAILS
        // =========================================================

        ProjectPaymentDetail paymentDetail = project.getPaymentDetail();

        if (paymentDetail != null) {

            double totalAmount = paymentDetail.getTotalAmount();
            double dueAmount = paymentDetail.getDueAmount();

            // Amount already received
            double paidAmount = totalAmount - dueAmount;

            dto.setTotalAmount(totalAmount);
            dto.setPaidAmount(paidAmount);
            dto.setDueAmount(dueAmount);

            if (paymentDetail.getPaymentType() != null) {
                dto.setPaymentTypeId(
                        paymentDetail.getPaymentType().getId()
                );

                dto.setPaymentTypeName(
                        paymentDetail.getPaymentType().getName()
                );
            }

            logger.debug(
                    "[PROJECT-PAYMENT-DETAILS] projectId={} | totalAmount={} | " +
                            "paidAmount={} | dueAmount={} | paymentType={}",
                    project.getId(),
                    totalAmount,
                    paidAmount,
                    dueAmount,
                    paymentDetail.getPaymentType() != null
                            ? paymentDetail.getPaymentType().getName()
                            : null
            );

        } else {

            dto.setTotalAmount(0.0);
            dto.setPaidAmount(0.0);
            dto.setDueAmount(0.0);
            dto.setPaymentTypeId(null);
            dto.setPaymentTypeName(null);
        }

        // =========================================================
        // CONTACT ACCESS
        // =========================================================

        User requestingUser = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> {
                    logger.warn(
                            "[MAP-PROJECT-DETAILS-USER-NOT-FOUND] userId={}",
                            userId
                    );

                    return new ResourceNotFoundException(
                            "User not found with ID: " + userId,
                            "ERR_USER_NOT_FOUND"
                    );
                });

        boolean isAdmin = hasRole(requestingUser, "ADMIN");

        boolean isOperationHead =
                hasRole(requestingUser, "OPERATION_HEAD");

        boolean belongsToCrtDepartment =
                requestingUser.getDepartments() != null
                        && requestingUser.getDepartments()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(Department::getName)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .anyMatch(name -> "CRT".equalsIgnoreCase(name));

        boolean canSeeFullContactInfo =
                isAdmin
                        || isOperationHead
                        || belongsToCrtDepartment;

        logger.info(
                "[CLIENT-CONTACT-VISIBILITY] projectId={} | userId={} | " +
                        "admin={} | operationHead={} | crtDepartment={} | " +
                        "fullContactAccess={}",
                project.getId(),
                userId,
                isAdmin,
                isOperationHead,
                belongsToCrtDepartment,
                canSeeFullContactInfo
        );

        List<ContactDetailsDto> contactDtos = new ArrayList<>();

        // =========================================================
        // UNIT CONTACTS
        // =========================================================

        if (project.getUnit() != null) {

            List<Contact> unitContacts =
                    contactRepository
                            .findByCompanyUnitIdAndIsDeletedFalseAndIsActiveTrue(
                                    project.getUnit().getId()
                            );

            for (Contact contact : unitContacts) {

                contactDtos.add(
                        buildContactDetailsDto(
                                contact,
                                canSeeFullContactInfo,
                                "Unit",
                                project.getUnit().getUnitName()
                        )
                );
            }
        }

        // =========================================================
        // COMPANY CONTACTS
        // =========================================================

        if (project.getCompany() != null) {

            List<Contact> companyContacts =
                    contactRepository
                            .findByCompanyIdAndCompanyUnitIsNullAndIsDeletedFalseAndIsActiveTrue(
                                    project.getCompany().getId()
                            );

            for (Contact contact : companyContacts) {

                boolean alreadyAdded = contactDtos.stream()
                        .anyMatch(existingContact ->
                                Objects.equals(
                                        existingContact.getId(),
                                        contact.getId()
                                )
                        );

                if (!alreadyAdded) {
                    contactDtos.add(
                            buildContactDetailsDto(
                                    contact,
                                    canSeeFullContactInfo,
                                    "Company",
                                    null
                            )
                    );
                }
            }
        }

        dto.setContacts(contactDtos);

        // =========================================================
        // PROCUREMENT ASSIGNMENT
        // =========================================================

        procurementMilestoneAssignmentRepository
                .findActiveByProjectIdNative(project.getId())
                .ifPresent(assignment ->
                        dto.setProcurementMilestoneAssignmentId(
                                assignment.getId()
                        )
                );

        logger.debug(
                "[MAP-PROJECT-DETAILS-SUCCESS] projectId={} | userId={} | " +
                        "contacts={} | fullContactAccess={}",
                project.getId(),
                userId,
                contactDtos.size(),
                canSeeFullContactInfo
        );

        return dto;
    }



    private ContactDetailsDto buildContactDetailsDto(
            Contact contact,
            boolean canSeeFullInfo,
            String level,
            String unitName
    ) {
        ContactDetailsDto dto = new ContactDetailsDto();

        dto.setId(contact.getId());
        dto.setTitle(contact.getTitle());
        dto.setName(contact.getName());

        dto.setDesignation(
                contact.getDesignation() != null
                        ? contact.getDesignation()
                        : contact.getClientDesignation()
        );

        dto.setLevel(level);
        dto.setUnitName(unitName);
        dto.setLevelDescription(contact.getLevelDescription());
        dto.setActive(contact.isActive());

        if (canSeeFullInfo) {
            dto.setEmails(contact.getEmail());
            dto.setContactNo(contact.getContactNo());
            dto.setWhatsappNo(contact.getWhatsappNo());
        } else {
            dto.setEmails(maskEmail(contact.getEmail()));
            dto.setContactNo(maskPhoneNumber(contact.getContactNo()));
            dto.setWhatsappNo(maskPhoneNumber(contact.getWhatsappNo()));
        }

        return dto;
    }




    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return phoneNumber;
        }
        String firstThree = phoneNumber.substring(0, 3);
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return firstThree + "XXXX" + lastFour;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];
        String maskedLocalPart = localPart.length() > 5 ? localPart.substring(0, 5) + "XXXXX" : localPart;
        int lastDotIndex = domainPart.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return maskedLocalPart + "@" + domainPart;
        }
        String domainPrefix = domainPart.substring(0, Math.min(3, lastDotIndex));
        String tld = domainPart.substring(lastDotIndex);
        return maskedLocalPart + "@" + domainPrefix + "XXX" + tld;
    }

    private AssignedMilestoneDto mapToAssignedMilestoneDto(ProjectMilestoneAssignment assignment) {
        AssignedMilestoneDto dto = new AssignedMilestoneDto();
        dto.setId(assignment.getId());
        dto.setProjectId(assignment.getProject().getId());
        dto.setProjectName(assignment.getProject().getName());
        dto.setMilestoneId(assignment.getMilestone().getId());
        dto.setMilestoneName(assignment.getMilestone().getName());
        dto.setStatus(assignment.getStatus().getName());
        dto.setStatusReason(assignment.getStatusReason());
        dto.setVisibilityReason(assignment.getVisibilityReason());
        dto.setReworkAttempts(assignment.getReworkAttempts());
        dto.setVisibleDate(assignment.getVisibleDate());
        dto.setStartedDate(assignment.getStartedDate());
        dto.setCompletedDate(assignment.getCompletedDate());

        dto.setAssignedUser(mapToUserResponseDto(assignment.getAssignedUser()));

        Milestone milestone = assignment.getMilestone();
        if (milestone != null && milestone.getDepartments() != null && !milestone.getDepartments().isEmpty()) {
            Department dept = milestone.getDepartments().get(0);
            dto.setDepartmentId(dept.getId());
            dto.setDepartmentName(dept.getName());

        } else {
            dto.setDepartmentId(null);
            dto.setDepartmentName(null);
        }

        return dto;
    }

    private UserResponseDto mapToUserResponseDto(User user) {
        if (user == null) {
            return null;
        }
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setContactNo(user.getContactNo());
        return dto;
    }



    @Override
    @Transactional
    public ProjectResponseDto addPaymentByUnbilledNumber(String unbilledNumber, ProjectPaymentTransactionDto dto) {
        validateTransactionDto(dto);
        Project project = projectRepository.findByUnbilledNumberAndIsDeletedFalse(unbilledNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));
        return addPaymentTransaction(project.getId(), dto);
    }

    private void validateProjectAllowsPayment(
            Project project
    ) {
        if (project == null || project.getStatus() == null) {
            throw new ValidationException(
                    "Project status is missing",
                    "ERR_PROJECT_STATUS_MISSING"
            );
        }

        Long statusId = project.getStatus().getId();

        if (StatusConstants.PROJECT_FORCE_CLOSED_ID
                .equals(statusId)) {

            throw new ValidationException(
                    "Payment cannot be added because project is FORCE_CLOSED",
                    "ERR_FORCE_CLOSED_PROJECT_PAYMENT_NOT_ALLOWED"
            );
        }

        if (StatusConstants.PROJECT_CANCELLED_ID
                .equals(statusId)
                || project.isCancelled()) {

            throw new ValidationException(
                    "Payment cannot be added because project is CANCELLED",
                    "ERR_CANCELLED_PROJECT_PAYMENT_NOT_ALLOWED"
            );
        }

        if (StatusConstants.PROJECT_REFUNDED_ID
                .equals(statusId)) {

            throw new ValidationException(
                    "Payment cannot be added because project is REFUNDED",
                    "ERR_REFUNDED_PROJECT_PAYMENT_NOT_ALLOWED"
            );
        }
    }
    @Override
    public List<DocumentChecklistDTO> getDocumentChecklist(Long projectId) {
        logger.info("Fetching document checklist for project ID: {}", projectId);

        Project project = projectRepository.findByIdWithApplicantTypeAndProduct(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));

        // If no Applicant Type → show dropdown
        if (project.getApplicantType() == null) {
            return Collections.emptyList();
        }

        // Required documents (Product + Applicant Type)
        List<ProductDocumentMapping> required = productDocumentMappingRepository
                .findByProductAndApplicantType(project.getProduct(), project.getApplicantType());

        // All uploaded documents for this project (from any milestone)
        List<ProjectDocumentUpload> uploaded = projectDocumentUploadRepository
                .findByProjectIdAndIsDeletedFalse(projectId);

        return required.stream().map(mapping -> {
                    DocumentChecklistDTO dto = new DocumentChecklistDTO();
                    ProductRequiredDocuments doc = mapping.getRequiredDocument();

                    dto.setDocumentId(doc.getId());
                    dto.setDocumentName(doc.getName());
                    dto.setMandatory(mapping.isMandatory());
                    dto.setDisplayOrder(mapping.getDisplayOrder());

                    uploaded.stream()
                            .filter(u -> u.getRequiredDocument().getId().equals(doc.getId()))
                            .findFirst()
                            .ifPresentOrElse(u -> {
                                dto.setStatus(u.getStatus().getName());
                                dto.setUploadId(u.getId());
                                dto.setFileUrl(u.getFileUrl());
                                dto.setUploadedAt(u.getUploadTime());
                                dto.setVerified("VERIFIED".equals(u.getStatus().getName()));
                                dto.setRemarks(u.getRemarks());
                            }, () -> dto.setStatus("PENDING"));

                    return dto;
                })
                .sorted(Comparator.comparingInt(d -> d.getDisplayOrder() != null ? d.getDisplayOrder() : 999))
                .collect(Collectors.toList());
    }


    @Override
    public ProjectHistoryResponseDto getProjectHistory(Long projectId) {
        logger.info("Fetching history for project ID: {}", projectId);

        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found  ", "ERR_PROJECT_NOT_FOUND"));

        User createdByUser = userRepository.findActiveUserById(project.getCreatedBy())
                .orElse(null); // In case user is deleted, handle gracefully

        ProjectHistoryResponseDto response = new ProjectHistoryResponseDto();
        response.setProjectId(project.getId());
        response.setProjectName(project.getName());
        response.setCreatedDate(project.getCreatedDate());
        response.setCreatedBy(project.getCreatedBy());
        response.setCreatedByName(createdByUser != null ? createdByUser.getFullName() : "Unknown");

        // Fetch all milestone assignments, sorted by order
        List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository
                .findByProjectIdAndIsDeletedFalse(projectId)
                .stream()
                .sorted(Comparator.comparing(a -> a.getProductMilestoneMap().getOrder()))
                .collect(Collectors.toList());

        List<MilestoneHistoryDto> milestoneHistories = assignments.stream()
                .map(this::mapToMilestoneHistoryDto)
                .collect(Collectors.toList());

        response.setMilestones(milestoneHistories);

        // Highlight the first milestone (smallest order)
        if (!milestoneHistories.isEmpty()) {
            logger.info("First milestone for project {}: {}", projectId, milestoneHistories.get(0).getMilestoneName());
        }

        return response;
    }

    private MilestoneHistoryDto mapToMilestoneHistoryDto(ProjectMilestoneAssignment assignment) {
        MilestoneHistoryDto dto = new MilestoneHistoryDto();
        dto.setMilestoneId(assignment.getMilestone().getId());
        dto.setMilestoneName(assignment.getMilestone().getName());
        dto.setOrder(assignment.getProductMilestoneMap().getOrder());
        dto.setAssignmentCreatedDate(assignment.getCreatedDate());

        // Assignment events from history
        List<ProjectAssignmentHistory> assignmentHistories = projectAssignmentHistoryRepository
                .findByMilestoneAssignmentIdAndIsDeletedFalse(assignment.getId())
                .stream()
                .sorted(Comparator.comparing(ProjectAssignmentHistory::getCreatedDate))
                .collect(Collectors.toList());

        List<AssignmentEventDto> assignmentEvents = assignmentHistories.stream()
                .map(this::mapToAssignmentEventDto)
                .collect(Collectors.toList());

        dto.setAssignmentEvents(assignmentEvents);

        // Status change events from history
        List<MilestoneStatusHistory> statusHistories = milestoneStatusHistoryRepository
                .findByMilestoneAssignmentIdAndIsDeletedFalse(assignment.getId())
                .stream()
                .sorted(Comparator.comparing(MilestoneStatusHistory::getChangeDate))
                .collect(Collectors.toList());

        List<StatusChangeEventDto> statusChangeEvents = statusHistories.stream()
                .map(this::mapToStatusChangeEventDto)
                .collect(Collectors.toList());

        // Include initial status if no history
        if (statusChangeEvents.isEmpty()) {
            StatusChangeEventDto initial = new StatusChangeEventDto();
            initial.setDate(assignment.getCreatedDate());
            initial.setPreviousStatus(null);
            initial.setNewStatus(assignment.getStatus().getName());
            initial.setChangedBy(assignment.getCreatedBy());
            User initialChangedBy = userRepository.findActiveUserById(assignment.getCreatedBy()).orElse(null);
            initial.setChangedByName(initialChangedBy != null ? initialChangedBy.getFullName() : "Unknown");
            initial.setReason("Initial status");
            statusChangeEvents.add(initial);
        }

        dto.setStatusChangeEvents(statusChangeEvents);

        return dto;
    }

    private AssignmentEventDto mapToAssignmentEventDto(ProjectAssignmentHistory history) {
        AssignmentEventDto dto = new AssignmentEventDto();
        dto.setDate(history.getCreatedDate());
        dto.setAssignedTo(history.getAssignedUser() != null ? history.getAssignedUser().getId() : null);
        dto.setAssignedToName(history.getAssignedUser() != null ? history.getAssignedUser().getFullName() : "Unassigned");
        dto.setAssignedBy(history.getCreatedBy());
        User assignedByUser = userRepository.findActiveUserById(history.getCreatedBy()).orElse(null);
        dto.setAssignedByName(assignedByUser != null ? assignedByUser.getFullName() : "Unknown");
        dto.setReason(history.getAssignmentReason());
        return dto;
    }

    private StatusChangeEventDto mapToStatusChangeEventDto(MilestoneStatusHistory history) {
        StatusChangeEventDto dto = new StatusChangeEventDto();
        dto.setDate(history.getChangeDate());
        dto.setPreviousStatus(history.getPreviousStatus().getName());
        dto.setNewStatus(history.getNewStatus().getName());
        dto.setChangedBy(history.getChangedBy() != null ? history.getChangedBy().getId() : null);
        dto.setChangedByName(history.getChangedBy() != null ? history.getChangedBy().getFullName() : "Unknown");
        dto.setReason(history.getChangeReason());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponseDto getProjectByUnbilledNumber(String unbilledNumber) {

        if (unbilledNumber == null || unbilledNumber.trim().isEmpty()) {
            throw new ValidationException(
                    "Unbilled number is required",
                    "ERR_UNBILLED_NUMBER_REQUIRED"
            );
        }

        Project project = projectRepository
                .findByUnbilledNumberAndIsDeletedFalse(unbilledNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));


        return mapToResponseDto(project);
    }

    @Override
    @Transactional
    public ProjectResponseDto cancelProjectByUnbilledNumber(Long userId, String unbilledNumber){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Approver not found.." ,
                        "USER_NOT_FOUND"
                ));

        Project project = projectRepository
                .findByUnbilledNumberAndIsDeletedFalse(unbilledNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));

        if (project.isCancelled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Project is already cancelled"
            );        }

        String previousStatus = project.getStatus() != null
                ? project.getStatus().getName()
                : null;

        // 🔥 Find or create CANCELLED status
        ProjectStatus cancelledStatus = projectStatusRepository
                .findByName("CANCELLED")
                .orElseGet(() -> {
                    ProjectStatus newStatus = new ProjectStatus();
                    newStatus.setName("CANCELLED");
                    newStatus.setDescription("Project has been cancelled");
                    return projectStatusRepository.save(newStatus);
                });

        project.setCancelled(true);
        project.setStatus(cancelledStatus);
        project.setCancellerId(user.getId());

        projectRepository.save(project);

        // =========================================================
        // PROJECT HISTORY - PROJECT CANCELLED
        // =========================================================
        saveProjectHistory(
                project,
                null,
                "PROJECT_CANCELLED",
                "PROJECT",
                project.getId(),
                "Project cancelled",
                "Project " + project.getProjectNo()
                        + " was cancelled by " + user.getFullName(),
                "Project cancelled",
                previousStatus,
                cancelledStatus.getName(),
                user.getId(),
                null,
                null,
                null
        );

        return mapToResponseDto(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesProjectStatusResponseDto> getSalesProjectStatusDashboard(
            Long userId,
            Long salesPersonId,
            String statusName,
            String search,
            int page,
            int size
    ) {
        User loggedInUser = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        boolean isAdmin = loggedInUser.getRoles()
                .stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));

        boolean isOperationHead = loggedInUser.getRoles()
                .stream()
                .anyMatch(r -> "OPERATION_HEAD".equalsIgnoreCase(r.getName()));

        /*
         * ADMIN / OPERATION_HEAD:
         * - Can see all projects.
         * - Can optionally filter by salesPersonId.
         *
         * Sales user:
         * - Can see only own projects where Project.salesPersonId = userId.
         */
        Long effectiveSalesPersonId;

        if (isAdmin || isOperationHead) {
            effectiveSalesPersonId = salesPersonId;
        } else {
            effectiveSalesPersonId = userId;
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<Project> projectPage = projectRepository.findSalesProjectStatusDashboard(
                effectiveSalesPersonId,
                normalize(statusName),
                normalize(search),
                pageable
        );

        List<Long> projectIds = projectPage.getContent()
                .stream()
                .map(Project::getId)
                .toList();

        Map<Long, List<ProjectMilestoneAssignment>> assignmentMap = new HashMap<>();

        if (!projectIds.isEmpty()) {
            List<ProjectMilestoneAssignment> assignments =
                    projectMilestoneAssignmentRepository.findDashboardAssignmentsByProjectIds(projectIds);

            assignmentMap = assignments.stream()
                    .collect(Collectors.groupingBy(a -> a.getProject().getId()));
        }

        Map<Long, List<ProjectMilestoneAssignment>> finalAssignmentMap = assignmentMap;

        return projectPage.map(project ->
                mapToSalesProjectStatusResponse(
                        project,
                        finalAssignmentMap.getOrDefault(project.getId(), List.of())
                )
        );
    }

    private SalesProjectStatusResponseDto mapToSalesProjectStatusResponse(
            Project project,
            List<ProjectMilestoneAssignment> assignments
    ) {
        long totalMilestones = assignments.size();

        long completedMilestones = assignments.stream()
                .filter(a -> isStatus(a, "COMPLETED"))
                .count();

        int completionPercentage = totalMilestones > 0
                ? (int) ((completedMilestones * 100) / totalMilestones)
                : 0;

        return SalesProjectStatusResponseDto.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .projectNo(project.getProjectNo())

                .productId(project.getProduct() != null ? project.getProduct().getId() : null)
                .productName(project.getProduct() != null ? project.getProduct().getProductName() : null)

                .companyId(project.getCompany() != null ? project.getCompany().getId() : null)
                .companyName(project.getCompany() != null ? project.getCompany().getName() : null)

                .unitId(project.getUnit() != null ? project.getUnit().getId() : null)
                .unitName(project.getUnit() != null ? project.getUnit().getUnitName() : null)

                .contactId(project.getContact() != null ? project.getContact().getId() : null)
                .contactName(project.getContact() != null ? project.getContact().getName() : null)

                .unbilledNumber(project.getUnbilledNumber())
                .estimateNumber(project.getEstimateNumber())

                .salesPersonId(project.getSalesPersonId())
                .salesPersonName(project.getSalesPersonName())

                .projectStatusId(project.getStatus() != null ? project.getStatus().getId() : null)
                .projectStatusName(project.getStatus() != null ? project.getStatus().getName() : null)

                .totalAmount(project.getPaymentDetail() != null ? project.getPaymentDetail().getTotalAmount() : 0.0)
                .dueAmount(project.getPaymentDetail() != null ? project.getPaymentDetail().getDueAmount() : 0.0)
                .paymentTypeName(
                        project.getPaymentDetail() != null
                                && project.getPaymentDetail().getPaymentType() != null
                                ? project.getPaymentDetail().getPaymentType().getName()
                                : null
                )

                .projectDate(project.getDate())
                .createdDate(project.getCreatedDate())
                .updatedDate(project.getUpdatedDate())

                .totalMilestones(totalMilestones)
                .completedMilestones(completedMilestones)
                .milestoneCompletionPercentage(completionPercentage)

                .departments(mapDepartmentWiseMilestones(assignments))
                .build();
    }
    private List<DepartmentWiseMilestoneDto> mapDepartmentWiseMilestones(
            List<ProjectMilestoneAssignment> assignments
    ) {
        Map<String, DepartmentWiseMilestoneDto> departmentMap = new LinkedHashMap<>();

        for (ProjectMilestoneAssignment assignment : assignments) {

            List<Department> departments =
                    assignment.getMilestone() != null
                            && assignment.getMilestone().getDepartments() != null
                            && !assignment.getMilestone().getDepartments().isEmpty()
                            ? assignment.getMilestone().getDepartments()
                            : List.of();

            MilestoneAssignmentStatusDto milestoneDto = mapMilestoneAssignmentStatusDto(assignment);

            if (departments.isEmpty()) {
                addMilestoneToDepartmentGroup(
                        departmentMap,
                        null,
                        "No Department",
                        milestoneDto
                );
            } else {
                for (Department department : departments) {
                    addMilestoneToDepartmentGroup(
                            departmentMap,
                            department.getId(),
                            department.getName(),
                            milestoneDto
                    );
                }
            }
        }

        return new ArrayList<>(departmentMap.values());
    }




    private MilestoneAssignmentStatusDto mapMilestoneAssignmentStatusDto(
            ProjectMilestoneAssignment assignment
    ) {
        User assignedUser = assignment.getAssignedUser();

        return MilestoneAssignmentStatusDto.builder()
                .assignmentId(assignment.getId())

                .milestoneId(assignment.getMilestone() != null ? assignment.getMilestone().getId() : null)
                .milestoneName(assignment.getMilestone() != null ? assignment.getMilestone().getName() : null)
                .milestoneOrder(
                        assignment.getProductMilestoneMap() != null
                                ? assignment.getProductMilestoneMap().getOrder()
                                : null
                )

                .milestoneStatusId(assignment.getStatus() != null ? assignment.getStatus().getId() : null)
                .milestoneStatusName(assignment.getStatus() != null ? assignment.getStatus().getName() : null)
                .statusReason(assignment.getStatusReason())

                .visible(assignment.isVisible())
                .visibilityReason(assignment.getVisibilityReason())

                .assignedUserId(assignedUser != null ? assignedUser.getId() : null)
                .assignedUserName(assignedUser != null ? assignedUser.getFullName() : null)
                .assignedUserEmail(assignedUser != null ? assignedUser.getEmail() : null)
                .assignedUserMobile(assignedUser != null ? assignedUser.getContactNo() : null)

                .visibleDate(assignment.getVisibleDate())
                .startedDate(assignment.getStartedDate())
                .completedDate(assignment.getCompletedDate())

                .reworkAttempts(assignment.getReworkAttempts())
                .build();
    }


    private boolean isStatus(ProjectMilestoneAssignment assignment, String statusName) {
        return assignment.getStatus() != null
                && assignment.getStatus().getName() != null
                && assignment.getStatus().getName().equalsIgnoreCase(statusName);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }


    private void addMilestoneToDepartmentGroup(
            Map<String, DepartmentWiseMilestoneDto> departmentMap,
            Long departmentId,
            String departmentName,
            MilestoneAssignmentStatusDto milestoneDto
    ) {
        String key = departmentId != null ? String.valueOf(departmentId) : "NO_DEPARTMENT";

        DepartmentWiseMilestoneDto departmentDto = departmentMap.computeIfAbsent(
                key,
                k -> DepartmentWiseMilestoneDto.builder()
                        .departmentId(departmentId)
                        .departmentName(departmentName)
                        .totalMilestones(0)
                        .completedMilestones(0)
                        .inProgressMilestones(0)
                        .pendingMilestones(0)
                        .milestones(new ArrayList<>())
                        .build()
        );

        departmentDto.getMilestones().add(milestoneDto);
        departmentDto.setTotalMilestones(departmentDto.getTotalMilestones() + 1);

        String status = milestoneDto.getMilestoneStatusName();

        if ("COMPLETED".equalsIgnoreCase(status)) {
            departmentDto.setCompletedMilestones(departmentDto.getCompletedMilestones() + 1);
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            departmentDto.setInProgressMilestones(departmentDto.getInProgressMilestones() + 1);
        } else {
            departmentDto.setPendingMilestones(departmentDto.getPendingMilestones() + 1);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProjectMilestoneAssignmentOptions(Long projectId) {

        if (projectId == null) {
            throw new ValidationException("Project ID is required", "ERR_PROJECT_ID_REQUIRED");
        }

        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));

        List<ProjectMilestoneAssignment> assignments =
                projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());

        return assignments.stream()
                .sorted(
                        Comparator
                                .comparingInt((ProjectMilestoneAssignment a) ->
                                        a.getProductMilestoneMap() != null
                                                ? a.getProductMilestoneMap().getOrder()
                                                : Integer.MAX_VALUE
                                )
                                .thenComparing(ProjectMilestoneAssignment::getId)
                )
                .map(assignment -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("assignmentId", assignment.getId());
                    map.put("milestoneName", getProjectMilestoneName(assignment));
                    return map;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String getProjectStatusByProjectNumber(String projectNumber) {

        if (projectNumber == null || projectNumber.trim().isEmpty()) {
            throw new ValidationException(
                    "Project number is required",
                    "PROJECT_NUMBER_REQUIRED"
            );
        }

        Project project = projectRepository
                .findByProjectNoIgnoreCaseAndIsDeletedFalse(projectNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with project number: " + projectNumber,
                        "PROJECT_NOT_FOUND"
                ));

        if (project.getStatus() == null) {
            return "STATUS_NOT_AVAILABLE";
        }

        return project.getStatus().getName();
    }

    private String getProjectMilestoneName(ProjectMilestoneAssignment assignment) {

        if (assignment == null) {
            return "Milestone";
        }

        if (assignment.getMilestone() != null
                && assignment.getMilestone().getName() != null
                && !assignment.getMilestone().getName().trim().isEmpty()) {
            return assignment.getMilestone().getName().trim();
        }

        if (assignment.getProductMilestoneMap() != null
                && assignment.getProductMilestoneMap().getMilestone() != null
                && assignment.getProductMilestoneMap().getMilestone().getName() != null
                && !assignment.getProductMilestoneMap().getMilestone().getName().trim().isEmpty()) {
            return assignment.getProductMilestoneMap().getMilestone().getName().trim();
        }

        return "Milestone-" + assignment.getId();
    }

    @Override
    public List<ProjectResponseDto> getProjectsByUnitId(Long unitId) {

        if (unitId == null) {
            throw new ValidationException(
                    "Unit ID is required",
                    "ERR_UNIT_ID_REQUIRED"
            );
        }

        List<Project> projects =
                projectRepository.findProjectsByUnitId(unitId);

        return projects.stream()
                .map(this::mapToResponseDto)
                .toList();
    }






    /**
     * Saves a row in the unified project history timeline.
     *
     * Existing project business logic is intentionally not changed here.
     * Event/reference enum names are resolved at runtime so this class does
     * not need hard references to every enum constant.
     */
    private void saveProjectHistory(
            Project project,
            ProjectMilestoneAssignment milestoneAssignment,
            String eventTypeName,
            String referenceTypeName,
            Long referenceId,
            String eventTitle,
            String description,
            String reason,
            String previousValue,
            String newValue,
            Long performedByUserId,
            Long triggeredByUserId,
            User previousAssignee,
            User newAssignee
    ) {
        if (project == null || project.getId() == null) {
            logger.warn(
                    "[PROJECT-HISTORY-SKIPPED] Project is null or not persisted"
            );
            return;
        }

        ProjectHistoryEventType eventType;
        try {
            eventType = ProjectHistoryEventType.valueOf(eventTypeName);
        } catch (IllegalArgumentException | NullPointerException ex) {
            logger.warn(
                    "[PROJECT-HISTORY-SKIPPED] Unknown eventType={} | projectId={}",
                    eventTypeName,
                    project.getId()
            );
            return;
        }

        ProjectHistoryReferenceType referenceType = null;
        if (referenceTypeName != null) {
            try {
                referenceType = ProjectHistoryReferenceType.valueOf(referenceTypeName);
            } catch (IllegalArgumentException ex) {
                logger.warn(
                        "[PROJECT-HISTORY-SKIPPED] Unknown referenceType={} | projectId={}",
                        referenceTypeName,
                        project.getId()
                );
                return;
            }
        }

        User performedByUser = null;
        if (performedByUserId != null) {
            performedByUser = userRepository
                    .findActiveUserById(performedByUserId)
                    .orElse(null);
        }

        User triggeredByUser = null;
        if (triggeredByUserId != null) {
            triggeredByUser = userRepository
                    .findActiveUserById(triggeredByUserId)
                    .orElse(null);
        }

        ProjectHistoryEvent historyEvent = new ProjectHistoryEvent();

        historyEvent.setProject(project);
        historyEvent.setMilestoneAssignment(milestoneAssignment);

        if (milestoneAssignment != null
                && milestoneAssignment.getMilestone() != null) {
            historyEvent.setMilestoneName(
                    milestoneAssignment.getMilestone().getName()
            );
        }

        historyEvent.setEventType(eventType);
        historyEvent.setReferenceType(referenceType);
        historyEvent.setReferenceId(referenceId);

        historyEvent.setEventTitle(eventTitle);
        historyEvent.setDescription(description);
        historyEvent.setReason(reason);

        historyEvent.setPreviousValue(previousValue);
        historyEvent.setNewValue(newValue);

        historyEvent.setPerformedByUserId(performedByUserId);
        historyEvent.setPerformedByName(
                performedByUser != null
                        ? performedByUser.getFullName()
                        : performedByUserId != null
                        ? "User #" + performedByUserId
                        : "System"
        );

        historyEvent.setTriggeredByUserId(triggeredByUserId);
        historyEvent.setTriggeredByName(
                triggeredByUser != null
                        ? triggeredByUser.getFullName()
                        : triggeredByUserId != null
                        ? "User #" + triggeredByUserId
                        : null
        );

        if (previousAssignee != null) {
            historyEvent.setPreviousAssigneeId(previousAssignee.getId());
            historyEvent.setPreviousAssigneeName(previousAssignee.getFullName());
        }

        if (newAssignee != null) {
            historyEvent.setNewAssigneeId(newAssignee.getId());
            historyEvent.setNewAssigneeName(newAssignee.getFullName());
        }

        historyEvent.setOccurredAt(LocalDateTime.now());

        projectHistoryEventRepository.save(historyEvent);

        logger.info(
                "[PROJECT-HISTORY-SAVED] projectId={} | eventType={} | referenceType={} | referenceId={} | performedByUserId={}",
                project.getId(),
                eventType,
                referenceType,
                referenceId,
                performedByUserId
        );
    }

}
