package com.doc.impl;

import com.doc.dto.contact.ContactDetailsDto;
import com.doc.dto.project.*;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.dto.user.UserResponseDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.Contact;
import com.doc.entity.client.PaymentType;
import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.project.*;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.user.Department;
import com.doc.entity.user.User;
import com.doc.entity.user.UserProductMap;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.*;
import com.doc.repository.documentRepo.DocumentStatusRepository;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.service.ProjectService;
import com.doc.validator.request.ProjectRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for managing project-related operations.
 */
@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    @Autowired
    private ProjectPaymentDetailRepository projectPaymentDetailRepository;

    @Autowired
    private ProjectPaymentTransactionRepository projectPaymentTransactionRepository;

    @Autowired
    private ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;

    @Autowired
    private ProjectAssignmentHistoryRepository projectAssignmentHistoryRepository;

    @Autowired
    private UserProjectCountRepository userProjectCountRepository;

    @Autowired
    private UserProductMapRepository userProductMapRepository;

    @Autowired
    private ProductMilestoneMapRepository productMilestoneMapRepository;

    @Autowired
    private ProjectDocumentUploadRepository projectDocumentUploadRepository;

    @Autowired
    private MilestoneStatusHistoryRepository milestoneStatusHistoryRepository;

    @Autowired
    private MilestoneStatusRepository milestoneStatusRepository;

    @Autowired
    private DocumentStatusRepository documentStatusRepository;

    @Autowired
    private ProjectStatusRepository projectStatusRepository;

    @Autowired
    private ProjectRequestValidator projectRequestValidator;

    private static class AssignmentResult {
        User user;
        String reason;

        AssignmentResult(User user, String reason) {
            this.user = user;
            this.reason = reason;
        }
    }

    @Override
    public ProjectResponseDto createProject(ProjectRequestDto requestDto) {
        logger.info("Creating project with projectNo: {}", requestDto.getProjectNo());
        projectRequestValidator.validate(requestDto);

        if (projectRepository.existsByProjectNoAndIsDeletedFalse(requestDto.getProjectNo().trim())) {
            logger.warn("Project number {} already exists", requestDto.getProjectNo());
            throw new ValidationException("Project with number " + requestDto.getProjectNo() + " already exists", "ERR_DUPLICATE_PROJECT_NO");
        }

        User salesPerson = userRepository.findActiveUserById(requestDto.getSalesPersonId())
                .orElseThrow(() -> {
                    logger.error("Sales person with ID {} not found or is deleted", requestDto.getSalesPersonId());
                    return new ResourceNotFoundException("Sales person with ID " + requestDto.getSalesPersonId() + " not found or is deleted", "ERR_SALES_PERSON_NOT_FOUND");
                });

        Product product = productRepository.findActiveUserById(requestDto.getProductId())
                .orElseThrow(() -> {
                    logger.error("Product with ID {} not found or is deleted", requestDto.getProductId());
                    return new ResourceNotFoundException("Product with ID " + requestDto.getProductId() + " not found or is deleted", "ERR_PRODUCT_NOT_FOUND");
                });

        Company company = companyRepository.findActiveUserById(requestDto.getCompanyId())
                .orElseThrow(() -> {
                    logger.error("Company with ID {} not found or is deleted", requestDto.getCompanyId());
                    return new ResourceNotFoundException("Company with ID " + requestDto.getCompanyId() + " not found or is deleted", "ERR_COMPANY_NOT_FOUND");
                });

        Contact contact = contactRepository.findByIdAndDeleteStatusFalseAndIsActiveTrue(requestDto.getContactId())
                .orElseThrow(() -> {
                    logger.error("Contact with ID {} not found or is deleted", requestDto.getContactId());
                    return new ResourceNotFoundException("Contact with ID " + requestDto.getContactId() + " not found or is deleted", "ERR_CONTACT_NOT_FOUND");
                });

        User createdBy = userRepository.findActiveUserById(requestDto.getCreatedBy())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", requestDto.getCreatedBy());
                    return new ResourceNotFoundException("User with ID " + requestDto.getCreatedBy() + " not found or is deleted", "ERR_USER_NOT_FOUND");
                });

        User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", requestDto.getUpdatedBy());
                    return new ResourceNotFoundException("User with ID " + requestDto.getUpdatedBy() + " not found or is deleted", "ERR_USER_NOT_FOUND");
                });

        User approvedBy = userRepository.findActiveUserById(requestDto.getApprovedById())
                .orElseThrow(() -> {
                    logger.error("Approved by user with ID {} not found or is deleted", requestDto.getApprovedById());
                    return new ResourceNotFoundException("Approved by user with ID " + requestDto.getApprovedById() + " not found or is deleted", "ERR_APPROVED_BY_NOT_FOUND");
                });

        PaymentType paymentType = paymentTypeRepository.findById(requestDto.getPaymentTypeId())
                .orElseThrow(() -> {
                    logger.error("Payment type with ID {} not found", requestDto.getPaymentTypeId());
                    return new ResourceNotFoundException("Payment type with ID " + requestDto.getPaymentTypeId() + " not found", "ERR_PAYMENT_TYPE_NOT_FOUND");
                });

        // Fetch milestones for the product
        List<ProductMilestoneMap> milestones = productMilestoneMapRepository.findByProductId(product.getId(),
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        if (milestones.isEmpty()) {
            logger.warn("No milestones found for product ID: {}", product.getId());
            throw new ValidationException("No milestones defined for product ID " + product.getId(), "ERR_NO_MILESTONES");
        }

        // Validate that each milestone (except auto-generated) has at least one associated department
        for (ProductMilestoneMap milestone : milestones) {
            if (!milestone.isAutoGenerated() && milestone.getMilestone().getDepartments().isEmpty()) {
                logger.warn("Milestone {} has no associated departments", milestone.getMilestone().getName());
                throw new ValidationException("Milestone " + milestone.getMilestone().getName() + " has no associated departments",
                        "ERR_NO_DEPARTMENTS_FOR_MILESTONE");
            }
        }

        double totalAmount = requestDto.getTotalAmount();
        double paidAmount = requestDto.getPaidAmount() != null ? requestDto.getPaidAmount() : 0.0;
        double dueAmount = totalAmount - paidAmount;

        // Validate initial payment based on payment type
        String paymentTypeName = paymentType.getName();
        if (paymentTypeName.equals("FULL")) {
            if (paidAmount != totalAmount) {
                logger.warn("FULL payment requires the entire amount ({}), received: {}", totalAmount, paidAmount);
                throw new ValidationException("FULL payment requires the entire amount of " + totalAmount, "ERR_INVALID_FULL_PAYMENT");
            }
        } else if (paymentTypeName.equals("PARTIAL")) {
            if (!(paidAmount > 0 && paidAmount < totalAmount)) {
                logger.warn("PARTIAL payment must be greater than 0 and less than total amount ({}), received: {}", totalAmount, paidAmount);
                throw new ValidationException("PARTIAL payment must be greater than 0 and less than total amount " + totalAmount, "ERR_INVALID_PARTIAL_PAYMENT");
            }
        } else if (paymentTypeName.equals("INSTALLMENT")) {
            if (paidAmount > totalAmount) {
                logger.warn("INSTALLMENT payment cannot exceed total amount ({}), received: {}", totalAmount, paidAmount);
                throw new ValidationException("Payment cannot exceed total amount " + totalAmount, "ERR_EXCEEDS_TOTAL");
            }
            // Allow 0 <= paidAmount <= totalAmount
        } else if (paymentTypeName.equals("PURCHASE_ORDER")) {
            if (paidAmount > 0) {
                logger.warn("No initial payment allowed for PURCHASE_ORDER at creation");
                throw new ValidationException("No initial payment allowed for PURCHASE_ORDER at project creation",
                        "ERR_INVALID_PO_PAYMENT");
            }
        }

        Project project = new Project();
        mapRequestDtoToProject(project, requestDto);
        project.setSalesPerson(salesPerson);
        project.setProduct(product);
        project.setCompany(company);
        project.setContact(contact);
        project.setCreatedBy(createdBy.getId());
        project.setUpdatedBy(updatedBy.getId());
        project.setCreatedDate(new Date());
        project.setUpdatedDate(new Date());
        project.setDeleted(false);
        project.setActive(true);
        project.setStatus(projectStatusRepository.findByName("OPEN")
                .orElseThrow(() -> new ResourceNotFoundException("Project status OPEN not found", "STATUS_NOT_FOUND")));

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

        project = projectRepository.save(project);
        logger.debug("Project saved with ID: {}", project.getId());

        // Record initial payment transaction if any payment was made
        if (paidAmount > 0) {
            ProjectPaymentTransaction transaction = new ProjectPaymentTransaction();
            transaction.setProject(project);
            transaction.setAmount(paidAmount);
            transaction.setTransactionDate(new Date());
            transaction.setCreatedBy(createdBy.getId());
            transaction.setCreatedDate(new Date());
            projectPaymentTransactionRepository.save(transaction);
            logger.debug("Initial payment transaction recorded for project ID: {}, amount: {}", project.getId(), paidAmount);
        }


        MilestoneStatus newStatusEntity = milestoneStatusRepository.findByName("NEW")
                .orElseThrow(() -> new ResourceNotFoundException("Milestone status NEW not found", "STATUS_NOT_FOUND"));

        // Create milestone assignments
        for (ProductMilestoneMap milestone : milestones) {
            ProjectMilestoneAssignment assignment = new ProjectMilestoneAssignment();
            assignment.setProject(project);
            assignment.setProductMilestoneMap(milestone);
            assignment.setMilestone(milestone.getMilestone());
            assignment.setStatus(newStatusEntity);
            assignment.setCreatedBy(createdBy.getId());
            assignment.setUpdatedBy(updatedBy.getId());
            assignment.setCreatedDate(new Date());
            assignment.setUpdatedDate(new Date());
            assignment.setDate(LocalDate.now());
            assignment.setDeleted(false);

            // Set visibility for PURCHASE_ORDER payment
            if (paymentTypeName.equals("PURCHASE_ORDER")) {
                if (milestone.getMilestone().getName().equalsIgnoreCase("Certification")) {
                    assignment.setVisible(false);
                    assignment.setVisibilityReason("Full payment required for Certification");
                } else {
                    assignment.setVisible(true);
                    assignment.setVisibilityReason(null);
                    assignment.setVisibleDate(new Date());
                }
            } else {
                assignment.setVisible(false);
                assignment.setVisibilityReason("Insufficient payment");
            }

            projectMilestoneAssignmentRepository.save(assignment);
            logger.debug("Milestone assignment created for project ID: {}, milestone: {}", project.getId(),
                    milestone.getMilestone().getName());
        }

        // Update milestone visibilities based on initial payment (also handles PURCHASE_ORDER logic)
        updateMilestoneVisibilities(project, createdBy.getId());

        logger.info("Project created successfully with projectNo: {}", requestDto.getProjectNo());
        return mapToResponseDto(project);
    }

    @Override
    public List<ProjectResponseDto> getAllProjects(Long userId, int page, int size) {
        logger.info("Fetching projects for user ID: {}, page: {}, size: {}", userId, page, size);
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found or is deleted", "ERR_USER_NOT_FOUND");
                });

        PageRequest pageable = PageRequest.of(page, size);
        Page<Project> projectPage;

        // Check if user has ADMIN role
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        if (isAdmin) {
            logger.debug("User ID: {} is ADMIN, fetching all projects", userId);
            projectPage = projectRepository.findByIsDeletedFalse(pageable);
        } else {
            List<Long> userIds = new ArrayList<>();
            userIds.add(userId);

            // If user is a manager, include subordinates
            if (user.isManagerFlag()) {
                logger.debug("User ID: {} is a manager, fetching subordinates' projects", userId);
                List<User> subordinates = userRepository.findByManagerIdAndIsDeletedFalse(userId);
                userIds.addAll(subordinates.stream().map(User::getId).collect(Collectors.toList()));
            }

            logger.debug("Fetching projects for user IDs: {}", userIds);
            projectPage = projectRepository.findByAssignedUserIds(userIds, pageable);
        }

        return projectPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteProject(Long id) {
        logger.info("Deleting project with ID: {}", id);
        Project project = projectRepository.findActiveUserById(id)
                .orElseThrow(() -> {
                    logger.error("Project with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Project with ID " + id + " not found or is deleted", "ERR_PROJECT_NOT_FOUND");
                });
        project.setDeleted(true);
        project.setUpdatedDate(new Date());
        project.getPaymentDetail().setDeleted(true);
        project.getPaymentDetail().setUpdatedDate(new Date());
        project.getMilestoneAssignments().forEach(assignment -> {
            assignment.setDeleted(true);
            assignment.setUpdatedDate(new Date());
        });
        projectRepository.save(project);
        logger.info("Project deleted successfully with ID: {}", id);
    }

    @Override
    public ProjectResponseDto addPaymentTransaction(Long projectId, ProjectPaymentTransactionDto transactionDto) {
        logger.info("Adding payment transaction for project ID: {}", projectId);
        validateTransactionDto(transactionDto);

        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> {
                    logger.error("Project with ID {} not found or is deleted", projectId);
                    return new ResourceNotFoundException("Project with ID " + projectId + " not found or is deleted", "ERR_PROJECT_NOT_FOUND");
                });

        ProjectPaymentDetail paymentDetail = projectPaymentDetailRepository.findByProjectIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> {
                    logger.error("Payment detail for project ID {} not found or is deleted", projectId);
                    return new ResourceNotFoundException("Payment detail for project ID " + projectId + " not found or is deleted", "ERR_PAYMENT_DETAIL_NOT_FOUND");
                });

        double amount = transactionDto.getAmount();
        String paymentTypeName = paymentDetail.getPaymentType().getName();
        double totalAmount = paymentDetail.getTotalAmount();
        double dueAmount = paymentDetail.getDueAmount();
        double paidAmount = totalAmount - dueAmount;

        // Validate payment amount
        if (amount <= 0) {
            logger.warn("Invalid payment amount: {}", amount);
            throw new ValidationException("Payment amount must be positive", "ERR_INVALID_PAYMENT_AMOUNT");
        }

        // Universal check: Payment amount cannot exceed due amount for any payment type
        if (amount > dueAmount) {
            logger.warn("Payment amount {} exceeds due amount {}", amount, dueAmount);
            throw new ValidationException("Payment amount cannot exceed due amount of " + dueAmount, "ERR_EXCEEDS_DUE_AMOUNT");
        }

        // Payment type-specific validations
        if (paymentTypeName.equals("Full Payment")) {
            // For Full Payment, the entire due amount must be paid in one transaction
            if (dueAmount > 0 && amount != dueAmount) {
                logger.warn("Full Payment requires the entire due amount ({}), received: {}", dueAmount, amount);
                throw new ValidationException("Full Payment requires the entire due amount of " + dueAmount, "ERR_INVALID_FULL_PAYMENT_AMOUNT");
            }
        } else if (paymentTypeName.equals("Purchase Order Payment")) {
            // For PO-based, ensure all non-Certification milestones are completed before accepting payment
            List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(projectId);
            boolean allNonCertificationCompleted = assignments.stream()
                    .filter(a -> !a.getMilestone().getName().equalsIgnoreCase("Certification"))
                    .allMatch(a -> "COMPLETED".equals(a.getStatus().getName()));
            if (!allNonCertificationCompleted) {
                logger.warn("Cannot add payment for PO-based project ID {} until all non-Certification milestones are completed", projectId);
                throw new ValidationException("All non-Certification milestones must be completed before adding payment for PO-based project", "ERR_PO_PAYMENT_MILESTONE_NOT_COMPLETED");
            }
        }

        // Update due amount
        paymentDetail.setDueAmount(dueAmount - amount);

        User createdBy = userRepository.findActiveUserById(transactionDto.getCreatedBy())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", transactionDto.getCreatedBy());
                    return new ResourceNotFoundException("User with ID " + transactionDto.getCreatedBy() + " not found or is deleted", "ERR_USER_NOT_FOUND");
                });

        ProjectPaymentTransaction transaction = new ProjectPaymentTransaction();
        transaction.setProject(project);
        transaction.setAmount(amount);
        transaction.setTransactionDate(transactionDto.getPaymentDate());
        transaction.setCreatedBy(createdBy.getId());
        transaction.setCreatedDate(new Date());

        paymentDetail.setUpdatedBy(createdBy.getId());
        paymentDetail.setUpdatedDate(new Date());

        projectPaymentTransactionRepository.save(transaction);
        projectPaymentDetailRepository.save(paymentDetail);
        logger.info("Transaction added for project ID: {}, amount: {}", projectId, amount);

        // Update milestone visibilities based on payment
        updateMilestoneVisibilities(project, createdBy.getId());

        return mapToResponseDto(project);
    }

    public void updateMilestoneStatus(Long assignmentId, String newStatusName, String statusReason, Long changedById) {
        logger.info("Updating milestone assignment ID: {} to status: {}", assignmentId, newStatusName);
        ProjectMilestoneAssignment assignment = projectMilestoneAssignmentRepository.findActiveUserById(assignmentId)
                .orElseThrow(() -> {
                    logger.error("Milestone assignment with ID {} not found or is deleted", assignmentId);
                    return new ResourceNotFoundException("Milestone assignment with ID " + assignmentId + " not found or is deleted", "ERR_MILESTONE_ASSIGNMENT_NOT_FOUND");
                });

        User changedBy = userRepository.findActiveUserById(changedById)
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", changedById);
                    return new ResourceNotFoundException("User with ID " + changedById + " not found or is deleted", "ERR_USER_NOT_FOUND");
                });

        MilestoneStatus newStatus = milestoneStatusRepository.findByName(newStatusName)
                .orElseThrow(() -> {
                    logger.error("Milestone status {} not found", newStatusName);
                    return new ResourceNotFoundException("Milestone status " + newStatusName + " not found", "STATUS_NOT_FOUND");
                });

        // Validate status transition
        validateMilestoneStatusTransition(assignment, newStatus, statusReason);

        // Validate documents for COMPLETED status
        if ("COMPLETED".equals(newStatus.getName())) {
            List<ProjectDocumentUpload> documents = projectDocumentUploadRepository.findByMilestoneAssignmentIdAndIsDeletedFalse(assignmentId);
            if (!assignment.getProductMilestoneMap().isAutoGenerated() && !documents.isEmpty()) {
                boolean allVerified = documents.stream().allMatch(doc -> "VERIFIED".equals(doc.getStatus().getName()));
                if (!allVerified) {
                    logger.warn("Cannot complete milestone ID: {} due to unverified documents", assignmentId);
                    throw new ValidationException("All documents must be verified to complete milestone", "ERR_UNVERIFIED_DOCUMENTS");
                }
            }
        }

        // Handle REJECTED → NEW for rework
        if ("NEW".equals(newStatus.getName()) && "REJECTED".equals(assignment.getStatus().getName())) {
            ProductMilestoneMap map = assignment.getProductMilestoneMap();
            if (!map.isAllowRollback()) {
                logger.warn("Rollback not allowed for milestone: {}", map.getMilestone().getName());
                throw new ValidationException("Rollback not allowed for milestone " + map.getMilestone().getName(), "ERR_ROLLBACK_NOT_ALLOWED");
            }
            if (assignment.getReworkAttempts() >= map.getMaxAttempts()) {
                logger.warn("Maximum rework attempts ({}) reached for milestone ID: {}", map.getMaxAttempts(), assignmentId);
                throw new ValidationException("Maximum rework attempts reached for milestone", "ERR_MAX_REWORK_ATTEMPTS");
            }
            assignment.setReworkAttempts(assignment.getReworkAttempts() + 1);
        }

        // Log status change
        MilestoneStatusHistory history = new MilestoneStatusHistory();
        history.setMilestoneAssignment(assignment);
        history.setPreviousStatus(assignment.getStatus());
        history.setNewStatus(newStatus);
        history.setChangeReason(statusReason);
        history.setChangedBy(changedBy);
        history.setChangeDate(new Date());
        history.setDeleted(false);

        // Update assignment
        assignment.setStatus(newStatus);
        assignment.setStatusReason(statusReason);
        if ("IN_PROGRESS".equals(newStatus.getName())) {
            assignment.setStartedDate(new Date());
        } else if ("COMPLETED".equals(newStatus.getName())) {
            assignment.setCompletedDate(new Date());
        } else if ("REJECTED".equals(newStatus.getName()) || "ON_HOLD".equals(newStatus.getName())) {
            assignment.setStatusReason(statusReason);
        }
        assignment.setUpdatedBy(changedById);
        assignment.setUpdatedDate(new Date());

        projectMilestoneAssignmentRepository.save(assignment);
        milestoneStatusHistoryRepository.save(history);
        logger.info("Milestone assignment ID: {} updated to status: {}", assignmentId, newStatus.getName());

        // Update project status
        updateProjectStatus(assignment.getProject(), changedById);

        // If completed, re-check visibilities to unlock/assign next
        if ("COMPLETED".equals(newStatus.getName())) {
            updateMilestoneVisibilities(assignment.getProject(), changedById);
        }
    }

    public void updateMilestoneVisibilities(Project project, Long updatedById) {
        logger.debug("Updating milestone visibilities for project ID: {}", project.getId());
        double totalAmount = project.getPaymentDetail().getTotalAmount();
        double paidAmount = totalAmount - project.getPaymentDetail().getDueAmount();
        double paidPercentage = (paidAmount / totalAmount) * 100.0;
        String paymentTypeName = project.getPaymentDetail().getPaymentType().getName();

        List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());
        if (assignments.isEmpty()) {
            logger.warn("No milestone assignments found for project ID: {}", project.getId());
            return;
        }

        assignments.sort(Comparator.comparing(a -> a.getProductMilestoneMap().getOrder()));

        if (paymentTypeName.equals("Purchase Order Payment")) {
            // For PO-based payment, all milestones except Certification are visible from the start
            for (ProjectMilestoneAssignment assignment : assignments) {
                ProductMilestoneMap map = assignment.getProductMilestoneMap();
                boolean isCertification = map.getMilestone().getName().equalsIgnoreCase("Certification");
                boolean isVisible;
                String visibilityReason = null;

                if (isCertification) {
                    // Certification milestone requires full payment
                    isVisible = Math.abs(project.getPaymentDetail().getDueAmount()) < 0.01;
                    visibilityReason = isVisible ? null : "Full payment required for Certification";
                } else {
                    // Non-Certification milestones are visible regardless of payment
                    isVisible = true;
                    visibilityReason = null;
                }

                if (assignment.isVisible() != isVisible || !Objects.equals(visibilityReason, assignment.getVisibilityReason())) {
                    assignment.setVisible(isVisible);
                    assignment.setVisibilityReason(visibilityReason);
                    assignment.setVisibleDate(isVisible ? new Date() : null);
                    assignment.setUpdatedBy(updatedById);
                    assignment.setUpdatedDate(new Date());
                    projectMilestoneAssignmentRepository.save(assignment);
                    logger.debug("Milestone {} (ID: {}) visibility updated to {}, reason: {}",
                            map.getMilestone().getName(), assignment.getId(), isVisible, visibilityReason);
                }

                if (isVisible && !map.isAutoGenerated() && assignment.getAssignedUser() == null) {
                    try {
                        AssignmentResult assignmentResult = assignMilestoneUser(map);
                        if (assignmentResult != null && assignmentResult.user != null) {
                            assignment.setAssignedUser(assignmentResult.user);
                            logger.debug("Assigned user {} (ID: {}) to milestone {} for project ID: {}",
                                    assignmentResult.user.getFullName(), assignmentResult.user.getId(),
                                    map.getMilestone().getName(), project.getId());

                            ProjectAssignmentHistory history = new ProjectAssignmentHistory();
                            history.setProject(project);
                            history.setMilestoneAssignment(assignment);
                            history.setAssignedUser(assignmentResult.user);
                            history.setAssignmentReason(assignmentResult.reason);
                            history.setCreatedDate(new Date());
                            history.setUpdatedDate(new Date());
                            history.setCreatedBy(updatedById);
                            history.setUpdatedBy(updatedById);
                            history.setDeleted(false);
                            projectAssignmentHistoryRepository.save(history);

                            UserProjectCount count = userProjectCountRepository.findByUserIdAndProductId(assignmentResult.user.getId(), project.getProduct().getId());
                            if (count == null) {
                                count = new UserProjectCount();
                                count.setUser(assignmentResult.user);
                                count.setProduct(project.getProduct());
                                count.setProjectCount(1);
                                count.setLastUpdatedDate(new Date());
                                count.setCreatedDate(new Date());
                                count.setUpdatedDate(new Date());
                                count.setCreatedBy(updatedById);
                                count.setUpdatedBy(updatedById);
                                count.setDeleted(false);
                            } else {
                                count.setProjectCount(count.getProjectCount() + 1);
                                count.setLastUpdatedDate(new Date());
                                count.setUpdatedDate(new Date());
                                count.setUpdatedBy(updatedById);
                            }
                            userProjectCountRepository.save(count);
                        }
                    } catch (ResourceNotFoundException e) {
                        logger.error("Failed to assign user to milestone {}: {}", map.getMilestone().getName(), e.getMessage());
                        throw e;
                    }
                }
            }
        } else {
            // Existing logic for other payment types (Full, Partial, Installment)
            double cumulativePaymentPercentage = 0.0;
            for (ProjectMilestoneAssignment assignment : assignments) {
                ProductMilestoneMap map = assignment.getProductMilestoneMap();
                cumulativePaymentPercentage += map.getPaymentPercentage();
                boolean allPreviousCompleted = true;

                for (ProjectMilestoneAssignment prevAssignment : assignments) {
                    if (prevAssignment.getProductMilestoneMap().getOrder() < map.getOrder()) {
                        if (!"COMPLETED".equals(prevAssignment.getStatus().getName())) {
                            allPreviousCompleted = false;
                            break;
                        }
                    }
                }

                boolean isVisible;
                String visibilityReason = null;
                if (map.getMilestone().getName().equalsIgnoreCase("Certification")) {
                    isVisible = Math.abs(project.getPaymentDetail().getDueAmount()) < 0.01;
                    visibilityReason = isVisible ? null : "Full payment required for Certification";
                } else {
                    isVisible = allPreviousCompleted && paidPercentage >= cumulativePaymentPercentage;
                    visibilityReason = !isVisible ? (allPreviousCompleted ? "Insufficient payment" : "Previous milestone incomplete") : null;
                }

                if (assignment.isVisible() != isVisible || !Objects.equals(visibilityReason, assignment.getVisibilityReason())) {
                    assignment.setVisible(isVisible);
                    assignment.setVisibilityReason(visibilityReason);
                    assignment.setVisibleDate(isVisible ? new Date() : null);
                    assignment.setUpdatedBy(updatedById);
                    assignment.setUpdatedDate(new Date());
                    projectMilestoneAssignmentRepository.save(assignment);
                    logger.debug("Milestone {} (ID: {}) visibility updated to {}, reason: {}",
                            map.getMilestone().getName(), assignment.getId(), isVisible, visibilityReason);
                }

                if (isVisible && !map.isAutoGenerated() && assignment.getAssignedUser() == null) {
                    try {
                        AssignmentResult assignmentResult = assignMilestoneUser(map);
                        if (assignmentResult != null && assignmentResult.user != null) {
                            assignment.setAssignedUser(assignmentResult.user);
                            logger.debug("Assigned user {} (ID: {}) to milestone {} for project ID: {}",
                                    assignmentResult.user.getFullName(), assignmentResult.user.getId(),
                                    map.getMilestone().getName(), project.getId());

                            ProjectAssignmentHistory history = new ProjectAssignmentHistory();
                            history.setProject(project);
                            history.setMilestoneAssignment(assignment);
                            history.setAssignedUser(assignmentResult.user);
                            history.setAssignmentReason(assignmentResult.reason);
                            history.setCreatedDate(new Date());
                            history.setUpdatedDate(new Date());
                            history.setCreatedBy(updatedById);
                            history.setUpdatedBy(updatedById);
                            history.setDeleted(false);
                            projectAssignmentHistoryRepository.save(history);

                            UserProjectCount count = userProjectCountRepository.findByUserIdAndProductId(assignmentResult.user.getId(), project.getProduct().getId());
                            if (count == null) {
                                count = new UserProjectCount();
                                count.setUser(assignmentResult.user);
                                count.setProduct(project.getProduct());
                                count.setProjectCount(1);
                                count.setLastUpdatedDate(new Date());
                                count.setCreatedDate(new Date());
                                count.setUpdatedDate(new Date());
                                count.setCreatedBy(updatedById);
                                count.setUpdatedBy(updatedById);
                                count.setDeleted(false);
                            } else {
                                count.setProjectCount(count.getProjectCount() + 1);
                                count.setLastUpdatedDate(new Date());
                                count.setUpdatedDate(new Date());
                                count.setUpdatedBy(updatedById);
                            }
                            userProjectCountRepository.save(count);
                        }
                    } catch (ResourceNotFoundException e) {
                        logger.error("Failed to assign user to milestone {}: {}", map.getMilestone().getName(), e.getMessage());
                        throw e;
                    }
                }
            }
        }
    }

    private void updateProjectStatus(Project project, Long updatedById) {
        logger.debug("Updating project status for project ID: {}", project.getId());
        List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());

        if (assignments.isEmpty()) {
            project.setStatus(projectStatusRepository.findByName("OPEN")
                    .orElseThrow(() -> new ResourceNotFoundException("Project status OPEN not found", "STATUS_NOT_FOUND")));
        } else if (assignments.stream().allMatch(a -> "COMPLETED".equals(a.getStatus().getName()))) {
            project.setStatus(projectStatusRepository.findByName("COMPLETED")
                    .orElseThrow(() -> new ResourceNotFoundException("Project status COMPLETED not found", "STATUS_NOT_FOUND")));
        } else if (assignments.stream().anyMatch(a -> "IN_PROGRESS".equals(a.getStatus().getName()) || "ON_HOLD".equals(a.getStatus().getName()))) {
            project.setStatus(projectStatusRepository.findByName("IN_PROGRESS")
                    .orElseThrow(() -> new ResourceNotFoundException("Project status IN_PROGRESS not found", "STATUS_NOT_FOUND")));
        } else {
            project.setStatus(projectStatusRepository.findByName("OPEN")
                    .orElseThrow(() -> new ResourceNotFoundException("Project status OPEN not found", "STATUS_NOT_FOUND")));
        }

        project.setUpdatedBy(updatedById);
        project.setUpdatedDate(new Date());
        projectRepository.save(project);
        logger.debug("Project ID: {} status updated to: {}", project.getId(), project.getStatus().getName());
    }

    private void validateMilestoneStatusTransition(ProjectMilestoneAssignment assignment, MilestoneStatus newStatus, String statusReason) {
        String newStatusName = newStatus.getName();
        if (statusReason == null || statusReason.trim().isEmpty()) {
            if ("COMPLETED".equals(newStatusName) || "ON_HOLD".equals(newStatusName) || "REJECTED".equals(newStatusName)) {
                logger.warn("Status reason is required for status: {}", newStatusName);
                throw new ValidationException("Status reason is required for status: " + newStatusName, "ERR_STATUS_REASON_REQUIRED");
            }
        }

        String currentStatusName = assignment.getStatus().getName();
        if (currentStatusName.equals(newStatusName)) {
            logger.warn("Milestone assignment ID: {} already in status: {}", assignment.getId(), newStatusName);
            throw new ValidationException("Milestone is already in status: " + newStatusName, "ERR_SAME_STATUS");
        }

        switch (currentStatusName) {
            case "NEW":
                if (!"IN_PROGRESS".equals(newStatusName) && !"ON_HOLD".equals(newStatusName)) {
                    throw new ValidationException("Invalid transition from NEW to " + newStatusName, "ERR_INVALID_STATUS_TRANSITION_NEW");
                }
                break;
            case "IN_PROGRESS":
                if (!"COMPLETED".equals(newStatusName) && !"ON_HOLD".equals(newStatusName) && !"REJECTED".equals(newStatusName)) {
                    throw new ValidationException("Invalid transition from IN_PROGRESS to " + newStatusName, "ERR_INVALID_STATUS_TRANSITION_IN_PROGRESS");
                }
                break;
            case "ON_HOLD":
                if (!"IN_PROGRESS".equals(newStatusName)) {
                    throw new ValidationException("Invalid transition from ON_HOLD to " + newStatusName, "ERR_INVALID_STATUS_TRANSITION_ON_HOLD");
                }
                break;
            case "REJECTED":
                if (!"NEW".equals(newStatusName)) {
                    throw new ValidationException("Invalid transition from REJECTED to " + newStatusName, "ERR_INVALID_STATUS_TRANSITION_REJECTED");
                }
                break;
            case "COMPLETED":
                throw new ValidationException("Cannot change status from COMPLETED", "ERR_STATUS_CHANGE_COMPLETED");
            default:
                throw new ValidationException("Invalid current status: " + currentStatusName, "ERR_INVALID_CURRENT_STATUS");
        }

        if (!assignment.isVisible() && !"NEW".equals(newStatusName)) {
            logger.warn("Cannot update milestone ID: {} to {} when not visible", assignment.getId(), newStatusName);
            throw new ValidationException("Milestone must be visible to change status to " + newStatusName, "ERR_MILESTONE_NOT_VISIBLE");
        }
    }

    private void validateTransactionDto(ProjectPaymentTransactionDto transactionDto) {
        if (transactionDto.getAmount() == null) {
            logger.warn("Transaction amount is null");
            throw new ValidationException("Transaction amount cannot be null", "ERR_NULL_AMOUNT");
        }
        if (transactionDto.getPaymentDate() == null) {
            logger.warn("Transaction date is null");
            throw new ValidationException("Transaction date cannot be null", "ERR_NULL_PAYMENT_DATE");
        }
        if (transactionDto.getCreatedBy() == null) {
            logger.warn("Created by user ID is null");
            throw new ValidationException("Created by user ID cannot be null", "ERR_NULL_CREATED_BY");
        }
    }

    private AssignmentResult assignMilestoneUser(ProductMilestoneMap milestone) {
        logger.info("Assigning user for milestone: {}, product ID: {}", milestone.getMilestone().getName(), milestone.getProduct().getId());

        if (milestone.isAutoGenerated()) {
            logger.debug("Milestone {} is auto-generated, no user assignment needed", milestone.getMilestone().getName());
            return null;
        }

        List<Department> milestoneDepartments = milestone.getMilestone().getDepartments();
        if (milestoneDepartments.isEmpty()) {
            logger.error("No departments associated with milestone: {}", milestone.getMilestone().getName());
            throw new ResourceNotFoundException("No departments associated with milestone " + milestone.getMilestone().getName(), "ERR_NO_DEPARTMENTS");
        }

        Department primaryDepartment = milestoneDepartments.get(0);
        logger.debug("Primary department for milestone {}: {} (ID: {})",
                milestone.getMilestone().getName(), primaryDepartment.getName(), primaryDepartment.getId());

        List<User> departmentUsers = userRepository.findByDepartmentsIdAndIsActiveTrueAndIsDeletedFalse(primaryDepartment.getId());
        if (departmentUsers.isEmpty()) {
            logger.warn("No users found in department {} (ID: {}) for milestone: {}",
                    primaryDepartment.getName(), primaryDepartment.getId(), milestone.getMilestone().getName());
            return assignAdmin(milestone);
        }

        List<UserProductMap> mappings = userProductMapRepository.findByProductIdAndIsDeletedFalse(milestone.getProduct().getId());
        List<UserProductMap> eligibleMappings = mappings.stream()
                .filter(m -> departmentUsers.stream().anyMatch(u -> u.getId().equals(m.getUser().getId())))
                .filter(m -> m.getRating() != null && m.getRating() > 0)
                .filter(m -> isUserAvailable(m.getUser()))
                .collect(Collectors.toList());

        if (eligibleMappings.isEmpty()) {
            logger.warn("No eligible users with positive ratings found for product ID: {} in department: {}",
                    milestone.getProduct().getId(), primaryDepartment.getName());
            return assignAdmin(milestone);
        }

        boolean allAssigned = eligibleMappings.stream().allMatch(UserProductMap::isAssigned);
        if (allAssigned) {
            logger.debug("All eligible users for product ID: {} are assigned, resetting isAssigned flags", milestone.getProduct().getId());
            eligibleMappings.forEach(m -> {
                m.setAssigned(false);
                userProductMapRepository.save(m);
            });
        }

        Optional<UserProductMap> selectedMappingOpt = eligibleMappings.stream()
                .filter(m -> !m.isAssigned())
                .max(Comparator.comparingDouble(m -> m.getRating() != null ? m.getRating() : 0.0));

        if (selectedMappingOpt.isPresent()) {
            UserProductMap selectedMapping = selectedMappingOpt.get();
            User selectedUser = selectedMapping.getUser();
            selectedMapping.setAssigned(true);
            userProductMapRepository.save(selectedMapping);
            logger.info("Assigned user: {} (ID: {}, Rating: {}) for milestone: {}",
                    selectedUser.getFullName(), selectedUser.getId(), selectedMapping.getRating(), milestone.getMilestone().getName());
            return new AssignmentResult(selectedUser, "Highest rating");
        }

        Optional<User> selectedManagerOpt = eligibleMappings.stream()
                .filter(m -> !m.isAssigned())
                .filter(m -> m.getUser().getManager() != null)
                .map(m -> m.getUser().getManager())
                .filter(this::isUserAvailable)
                .findFirst();

        if (selectedManagerOpt.isPresent()) {
            User manager = selectedManagerOpt.get();
            UserProductMap managerProductMap = userProductMapRepository.findByUserIdAndProductIdAndIsDeletedFalse(
                    manager.getId(), milestone.getProduct().getId()).orElseGet(() -> {
                UserProductMap newMap = new UserProductMap();
                newMap.setUser(manager);
                newMap.setProduct(milestone.getProduct());
                newMap.setRating(0.0);
                newMap.setAssigned(false);
                newMap.setDeleted(false);
                newMap.setCreatedDate(new Date());
                newMap.setUpdatedDate(new Date());
                newMap.setCreatedBy(0L);
                newMap.setUpdatedBy(0L);
                return newMap;
            });
            managerProductMap.setAssigned(true);
            userProductMapRepository.save(managerProductMap);
            logger.info("Assigned manager: {} (ID: {}) for milestone: {}",
                    manager.getFullName(), manager.getId(), milestone.getMilestone().getName());
            return new AssignmentResult(manager, "Manager assigned due to no available users");
        }

        return assignAdmin(milestone);
    }

    private AssignmentResult assignAdmin(ProductMilestoneMap milestone) {
        List<User> admins = userRepository.findAdmins();
        Optional<User> availableAdmin = admins.stream()
                .filter(this::isUserAvailable)
                .findFirst();

        if (availableAdmin.isPresent()) {
            User admin = availableAdmin.get();
            UserProductMap adminProductMap = userProductMapRepository.findByUserIdAndProductIdAndIsDeletedFalse(
                    admin.getId(), milestone.getProduct().getId()).orElseGet(() -> {
                UserProductMap newMap = new UserProductMap();
                newMap.setUser(admin);
                newMap.setProduct(milestone.getProduct());
                newMap.setRating(0.0);
                newMap.setAssigned(false);
                newMap.setDeleted(false);
                newMap.setCreatedDate(new Date());
                newMap.setUpdatedDate(new Date());
                newMap.setCreatedBy(0L);
                newMap.setUpdatedBy(0L);
                return newMap;
            });
            adminProductMap.setAssigned(true);
            userProductMapRepository.save(adminProductMap);
            logger.info("Assigned admin: {} (ID: {}) for milestone: {}",
                    admin.getFullName(), admin.getId(), milestone.getMilestone().getName());
            return new AssignmentResult(admin, "Admin assigned due to no available users or managers");
        }

        logger.error("No available admins found for milestone: {}", milestone.getMilestone().getName());
        throw new ResourceNotFoundException("No available users, managers, or admins found for milestone " + milestone.getMilestone().getName(), "ERR_NO_ADMINS");
    }

    private boolean isUserAvailable(User user) {
        boolean available = user != null && !user.isDeleted();
        logger.debug("Checking user availability: {} (ID: {}), available: {}",
                user != null ? user.getFullName() : "null", user != null ? user.getId() : "null", available);
        return available;
    }

    private void mapRequestDtoToProject(Project project, ProjectRequestDto requestDto) {
        project.setName(requestDto.getName().trim());
        project.setProjectNo(requestDto.getProjectNo().trim());
        project.setLeadId(requestDto.getLeadId());
        project.setDate(requestDto.getDate());
        project.setAddress(requestDto.getAddress());
        project.setCity(requestDto.getCity());
        project.setState(requestDto.getState());
        project.setCountry(requestDto.getCountry());
        project.setPrimaryPinCode(requestDto.getPrimaryPinCode());
    }

    private ProjectResponseDto mapToResponseDto(Project project) {
        ProjectResponseDto dto = new ProjectResponseDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setProjectNo(project.getProjectNo());
        dto.setSalesPersonId(project.getSalesPerson() != null ? project.getSalesPerson().getId() : null);
        dto.setProductId(project.getProduct() != null ? project.getProduct().getId() : null);
        dto.setCompanyId(project.getCompany() != null ? project.getCompany().getId() : null);
        dto.setContactId(project.getContact() != null ? project.getContact().getId() : null);
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

    @Override
    public Page<AssignedProjectResponseDto> getAssignedProjects(Long userId, int page, int size) {
        logger.info("Fetching assigned projects with milestones for user ID: {}, page: {}, size: {}", userId, page, size);
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found or is deleted", "ERR_USER_NOT_FOUND"));

        PageRequest pageable = PageRequest.of(page, size * 10); // Adjust size to account for multiple assignments per project

        Page<ProjectMilestoneAssignment> assignmentPage;

        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        boolean isOperationHead = user.getRoles().stream().anyMatch(role -> role.getName().equals("OPERATION_HEAD"));

        if (isAdmin || isOperationHead) {
            assignmentPage = projectMilestoneAssignmentRepository.findAllByIsDeletedFalse(pageable);
        } else if (user.isManagerFlag()) {
            List<Department> managerDepts = user.getDepartments();
            if (managerDepts.isEmpty()) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
            List<Long> deptIds = managerDepts.stream().map(Department::getId).collect(Collectors.toList());
            List<User> deptUsers = userRepository.findByDepartmentIdsIn(deptIds);
            List<Long> deptUserIds = deptUsers.stream().map(User::getId).collect(Collectors.toList());
            if (!deptUserIds.contains(userId)) {
                deptUserIds.add(userId);
            }
            assignmentPage = projectMilestoneAssignmentRepository.findByAssignedUserIdInAndIsVisibleTrueAndStatusIn(
                    deptUserIds, Arrays.asList(milestoneStatusRepository.findByName("NEW").orElseThrow(), milestoneStatusRepository.findByName("IN_PROGRESS").orElseThrow()), pageable);
        } else {
            assignmentPage = projectMilestoneAssignmentRepository.findByAssignedUserIdAndIsVisibleTrueAndStatusIn(
                    userId, Arrays.asList(milestoneStatusRepository.findByName("NEW").orElseThrow(), milestoneStatusRepository.findByName("IN_PROGRESS").orElseThrow()), pageable);
        }

        List<ProjectMilestoneAssignment> assignments = assignmentPage.getContent();
        for (ProjectMilestoneAssignment assignment : assignments) {
            assignment.setDocuments(projectDocumentUploadRepository.findByMilestoneAssignmentIdAndIsDeletedFalse(assignment.getId()));
        }

        Map<Project, List<ProjectMilestoneAssignment>> groupedByProject = assignments.stream()
                .collect(Collectors.groupingBy(ProjectMilestoneAssignment::getProject));

        List<AssignedProjectResponseDto> projectDtos = new ArrayList<>();
        for (Map.Entry<Project, List<ProjectMilestoneAssignment>> entry : groupedByProject.entrySet()) {
            AssignedProjectResponseDto dto = new AssignedProjectResponseDto();
            dto.setProject(mapToProjectDetailsDto(entry.getKey(), userId));
            projectDtos.add(dto);
        }

        int start = Math.min(page * size, projectDtos.size());
        int end = Math.min(start + size, projectDtos.size());
        List<AssignedProjectResponseDto> pagedDtos = projectDtos.subList(start, end);
        return new PageImpl<>(pagedDtos, PageRequest.of(page, size), projectDtos.size());
    }

    @Override
    public ProjectMilestoneResponseDto getProjectMilestones(Long projectId, Long userId) {
        logger.info("Fetching project details and milestones for project ID: {}, user ID: {}", projectId, userId);
        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> {
                    logger.error("Project with ID {} not found or is deleted", projectId);
                    return new ResourceNotFoundException("Project with ID " + projectId + " not found or is deleted", "ERR_PROJECT_NOT_FOUND");
                });

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found or is deleted", "ERR_USER_NOT_FOUND");
                });

        // Check authorization
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        boolean isOperationHead = user.getRoles().stream().anyMatch(role -> role.getName().equals("OPERATION_HEAD"));
        boolean isAssignedUser = projectMilestoneAssignmentRepository.findByProjectIdAndAssignedUserIdAndIsDeletedFalse(projectId, userId).isPresent();
        boolean isManagerOfAssignedUser = false;

        if (!isAdmin && !isOperationHead && !isAssignedUser) {
            // Check if the user is a manager of any assigned user
            List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(projectId);
            List<Long> assignedUserIds = assignments.stream()
                    .filter(a -> a.getAssignedUser() != null)
                    .map(a -> a.getAssignedUser().getId())
                    .collect(Collectors.toList());
            List<User> managedUsers = userRepository.findByManagerIdAndIsDeletedFalse(userId);
            isManagerOfAssignedUser = managedUsers.stream().anyMatch(u -> assignedUserIds.contains(u.getId()));
        }

        if (!isAdmin && !isOperationHead && !isAssignedUser && !isManagerOfAssignedUser) {
            logger.warn("User ID: {} is not authorized to view project ID: {}", userId, projectId);
            throw new ValidationException("User is not authorized to view this project", "ERR_UNAUTHORIZED_ACCESS");
        }

        List<ProjectMilestoneAssignment> assignments;

        if (isAdmin || isOperationHead) {
            assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(projectId);
        } else if (isManagerOfAssignedUser) {
            List<Long> managedUserIds = userRepository.findByManagerIdAndIsDeletedFalse(userId)
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            if (!managedUserIds.contains(userId)) {
                managedUserIds.add(userId); // Include the manager themselves if they are assigned
            }
            assignments = projectMilestoneAssignmentRepository.findByProjectIdAndAssignedUserIdInAndIsVisibleTrueAndStatusIn(
                    projectId, managedUserIds, Arrays.asList(milestoneStatusRepository.findByName("NEW").orElseThrow(), milestoneStatusRepository.findByName("IN_PROGRESS").orElseThrow()));
        } else {
            assignments = projectMilestoneAssignmentRepository.findByProjectIdAndAssignedUserIdAndIsVisibleTrueAndStatusIn(
                    projectId, userId, Arrays.asList(milestoneStatusRepository.findByName("NEW").orElseThrow(), milestoneStatusRepository.findByName("IN_PROGRESS").orElseThrow()));
        }

        for (ProjectMilestoneAssignment assignment : assignments) {
            assignment.setDocuments(projectDocumentUploadRepository.findByMilestoneAssignmentIdAndIsDeletedFalse(assignment.getId()));
        }

        ProjectMilestoneResponseDto response = new ProjectMilestoneResponseDto();
        response.setProjectDetails(mapToProjectDetailsDto(project, userId));
        response.setMilestones(assignments.stream()
                .map(this::mapToAssignedMilestoneDto)
                .collect(Collectors.toList()));

        return response;
    }

    private ProjectDetailsDto mapToProjectDetailsDto(Project project, Long userId) {
        ProjectDetailsDto dto = new ProjectDetailsDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setProjectNo(project.getProjectNo());
        dto.setDate(project.getDate());
        dto.setAddress(project.getAddress());
        dto.setCity(project.getCity());
        dto.setState(project.getState());
        dto.setCountry(project.getCountry());
        dto.setProductId(project.getProduct() != null ? project.getProduct().getId() : null);
        dto.setProductName(project.getProduct() != null ? project.getProduct().getProductName() : null);
        dto.setCompanyId(project.getCompany() != null ? project.getCompany().getId() : null);
        dto.setCompanyName(project.getCompany() != null ? project.getCompany().getName() : null);
        dto.setCreatedDate(project.getCreatedDate());
        dto.setUpdatedDate(project.getUpdatedDate());

        // Fetch and map contact details based on user role and department
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found or is deleted", "ERR_USER_NOT_FOUND"));
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        boolean isOperationHead = user.getRoles().stream().anyMatch(role -> role.getName().equals("OPERATION_HEAD"));
        boolean isCrtDepartment = user.getDepartments().stream()
                .anyMatch(dept -> dept.getName().equalsIgnoreCase("CRT"));

        List<ContactDetailsDto> contactDtos = new ArrayList<>();
        if (project.getCompany() != null) {
            List<Contact> contacts = project.getCompany().getContacts().stream()
                    .filter(contact -> !contact.isDeleteStatus())
                    .collect(Collectors.toList());
            for (Contact contact : contacts) {
                ContactDetailsDto contactDto = new ContactDetailsDto();
                contactDto.setId(contact.getId());
                contactDto.setName(contact.getName());
                contactDto.setDesignation(contact.getDesignation());

                if (isAdmin || isOperationHead || isCrtDepartment) {
                    // ADMIN, OPERATION_HEAD, or CRT department sees unmasked details
                    contactDto.setEmails(contact.getEmails());
                    contactDto.setContactNo(contact.getContactNo());
                    contactDto.setWhatsappNo(contact.getWhatsappNo());
                } else {
                    // Non-ADMIN, non-OPERATION_HEAD, non-CRT department sees masked details
                    contactDto.setEmails(maskEmail(contact.getEmails()));
                    contactDto.setContactNo(maskPhoneNumber(contact.getContactNo()));
                    contactDto.setWhatsappNo(maskPhoneNumber(contact.getWhatsappNo()));
                }
                contactDtos.add(contactDto);
            }
        }
        dto.setContacts(contactDtos);

        return dto;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return phoneNumber; // Return as-is if too short to mask
        }
        // Show first 3 and last 4 digits, mask middle with "XXXX"
        String firstThree = phoneNumber.substring(0, 3);
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return firstThree + "XXXX" + lastFour;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email; // Return as-is if invalid email
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];
        // Mask local part: keep first 5 chars, replace rest with "XXXXX"
        String maskedLocalPart = localPart.length() > 5 ? localPart.substring(0, 5) + "XXXXX" : localPart;
        // Mask domain: keep first 3 chars and TLD, mask middle with "XXX"
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
        dto.setDocuments(assignment.getDocuments().stream()
                .map(this::mapToDocumentResponseDto)
                .collect(Collectors.toList()));
        dto.setAssignedUser(mapToUserResponseDto(assignment.getAssignedUser()));
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

    private DocumentResponseDto mapToDocumentResponseDto(ProjectDocumentUpload document) {
        DocumentResponseDto dto = new DocumentResponseDto();
        dto.setId(document.getId());
        dto.setFileUrl(document.getFileUrl());
        dto.setFileName(document.getFileName());
        dto.setOldFileUrl(document.getOldFileUrl());
        dto.setOldFileName(document.getOldFileName());
        dto.setStatus(document.getStatus());
        dto.setRemarks(document.getRemarks());
        dto.setUploadTime(document.getUploadTime());
        dto.setRequiredDocumentId(document.getRequiredDocument().getId());
        dto.setMilestoneAssignmentId(document.getMilestoneAssignment().getId());
        dto.setProjectId(document.getProject().getId());
        dto.setUploadedById(document.getUploadedBy().getId());
        dto.setCreatedDate(document.getCreatedDate());
        dto.setUpdatedDate(document.getUpdatedDate());
        dto.setReplacementCount(document.getReplacementCount());
        return dto;
    }
}