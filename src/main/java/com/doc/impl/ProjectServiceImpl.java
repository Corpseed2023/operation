package com.doc.impl;

import com.doc.dto.project.*;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.dto.user.UserResponseDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.Contact;
import com.doc.entity.client.PaymentType;
import com.doc.entity.project.*;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.user.Department;
import com.doc.entity.user.User;
import com.doc.entity.user.UserLoginStatus;
import com.doc.entity.user.UserProductMap;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.*;
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
    private UserProductMapRepository userProductMapRepository;

    @Autowired
    private ProductMilestoneMapRepository productMilestoneMapRepository;

    @Autowired
    private ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;

    @Autowired
    private ProjectAssignmentHistoryRepository projectAssignmentHistoryRepository;

    @Autowired
    private UserProjectCountRepository userProjectCountRepository;

    @Autowired
    private UserLoginStatusRepository userOnlineStatusRepository;

    @Autowired
    private ProjectDocumentUploadRepository projectDocumentUploadRepository;

    @Autowired
    private MilestoneStatusHistoryRepository milestoneStatusHistoryRepository;

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
            throw new ValidationException("Project with number " + requestDto.getProjectNo() + " already exists");
        }

        User salesPerson = userRepository.findByIdAndIsDeletedFalse(requestDto.getSalesPersonId())
                .orElseThrow(() -> {
                    logger.error("Sales person with ID {} not found or is deleted", requestDto.getSalesPersonId());
                    return new ResourceNotFoundException("Sales person with ID " + requestDto.getSalesPersonId() + " not found or is deleted");
                });

        Product product = productRepository.findByIdAndIsDeletedFalse(requestDto.getProductId())
                .orElseThrow(() -> {
                    logger.error("Product with ID {} not found or is deleted", requestDto.getProductId());
                    return new ResourceNotFoundException("Product with ID " + requestDto.getProductId() + " not found or is deleted");
                });

        Company company = companyRepository.findByIdAndIsDeletedFalse(requestDto.getCompanyId())
                .orElseThrow(() -> {
                    logger.error("Company with ID {} not found or is deleted", requestDto.getCompanyId());
                    return new ResourceNotFoundException("Company with ID " + requestDto.getCompanyId() + " not found or is deleted");
                });

        Contact contact = contactRepository.findByIdAndDeleteStatusFalse(requestDto.getContactId())
                .orElseThrow(() -> {
                    logger.error("Contact with ID {} not found or is deleted", requestDto.getContactId());
                    return new ResourceNotFoundException("Contact with ID " + requestDto.getContactId() + " not found or is deleted");
                });

        User createdBy = userRepository.findByIdAndIsDeletedFalse(requestDto.getCreatedBy())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", requestDto.getCreatedBy());
                    return new ResourceNotFoundException("User with ID " + requestDto.getCreatedBy() + " not found or is deleted");
                });

        User updatedBy = userRepository.findByIdAndIsDeletedFalse(requestDto.getUpdatedBy())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", requestDto.getUpdatedBy());
                    return new ResourceNotFoundException("User with ID " + requestDto.getUpdatedBy() + " not found or is deleted");
                });

        User approvedBy = userRepository.findByIdAndIsDeletedFalse(requestDto.getApprovedById())
                .orElseThrow(() -> {
                    logger.error("Approved by user with ID {} not found or is deleted", requestDto.getApprovedById());
                    return new ResourceNotFoundException("Approved by user with ID " + requestDto.getApprovedById() + " not found or is deleted");
                });

        PaymentType paymentType = paymentTypeRepository.findById(requestDto.getPaymentTypeId())
                .orElseThrow(() -> {
                    logger.error("Payment type with ID {} not found", requestDto.getPaymentTypeId());
                    return new ResourceNotFoundException("Payment type with ID " + requestDto.getPaymentTypeId() + " not found");
                });

        // Fetch milestones for the product
        List<ProductMilestoneMap> milestones = productMilestoneMapRepository.findByProductId(product.getId(),
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        if (milestones.isEmpty()) {
            logger.warn("No milestones found for product ID: {}", product.getId());
            throw new ValidationException("No milestones defined for product ID " + product.getId());
        }

        // Validate that each milestone (except auto-generated) has at least one associated department
        for (ProductMilestoneMap milestone : milestones) {
            if (!milestone.isAutoGenerated() && milestone.getMilestone().getDepartments().isEmpty()) {
                logger.warn("Milestone {} has no associated departments", milestone.getMilestone().getName());
                throw new ValidationException("Milestone " + milestone.getMilestone().getName() + " has no associated departments");
            }
        }

        double totalAmount = requestDto.getTotalAmount();
        double paidAmount = requestDto.getPaidAmount() != null ? requestDto.getPaidAmount() : 0.0;
        double dueAmount = totalAmount - paidAmount;

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
        project.setStatus(ProjectStatus.OPEN);

        ProjectPaymentDetail paymentDetail = new ProjectPaymentDetail();
        paymentDetail.setProject(project);
        paymentDetail.setTotalAmount(totalAmount);
        paymentDetail.setDueAmount(dueAmount);
        paymentDetail.setPaymentStatus(requestDto.getPaymentStatus());
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

        // Create milestone assignments
        for (ProductMilestoneMap milestone : milestones) {
            ProjectMilestoneAssignment assignment = new ProjectMilestoneAssignment();
            assignment.setProject(project);
            assignment.setProductMilestoneMap(milestone);
            assignment.setMilestone(milestone.getMilestone());
            assignment.setStatus(MilestoneStatus.NEW);
            assignment.setVisible(false);
            assignment.setVisibilityReason("Insufficient payment");
            assignment.setCreatedBy(createdBy.getId());
            assignment.setUpdatedBy(updatedBy.getId());
            assignment.setCreatedDate(new Date());
            assignment.setUpdatedDate(new Date());
            assignment.setDate(LocalDate.now());
            assignment.setDeleted(false);

            projectMilestoneAssignmentRepository.save(assignment);
            logger.debug("Milestone assignment created for project ID: {}, milestone: {}", project.getId(), milestone.getMilestone().getName());
        }

        // Update milestone visibilities based on initial payment
        updateMilestoneVisibilities(project, createdBy.getId());

        logger.info("Project created successfully with projectNo: {}", requestDto.getProjectNo());
        return mapToResponseDto(project);
    }

    @Override
    public ProjectResponseDto getProjectById(Long id) {
        logger.info("Fetching project with ID: {}", id);
        Project project = projectRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.error("Project with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Project with ID " + id + " not found or is deleted");
                });
        return mapToResponseDto(project);
    }

    @Override
    public List<ProjectResponseDto> getAllProjects(Long userId, int page, int size) {
        logger.info("Fetching projects for user ID: {}, page: {}, size: {}", userId, page, size);
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found or is deleted");
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
    public ProjectResponseDto updateProject(Long id, ProjectRequestDto requestDto) {
        logger.info("Updating project with ID: {}", id);
        projectRequestValidator.validate(requestDto);

        Project project = projectRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.error("Project with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Project with ID " + id + " not found or is deleted");
                });

        if (!project.getProjectNo().equals(requestDto.getProjectNo().trim()) &&
                projectRepository.existsByProjectNoAndIsDeletedFalse(requestDto.getProjectNo().trim())) {
            logger.warn("Project number {} already exists", requestDto.getProjectNo());
            throw new ValidationException("Project with number " + requestDto.getProjectNo() + " already exists");
        }

        User salesPerson = userRepository.findByIdAndIsDeletedFalse(requestDto.getSalesPersonId())
                .orElseThrow(() -> {
                    logger.error("Sales person with ID {} not found or is deleted", requestDto.getSalesPersonId());
                    return new ResourceNotFoundException("Sales person with ID " + requestDto.getSalesPersonId() + " not found or is deleted");
                });

        Product product = productRepository.findByIdAndIsDeletedFalse(requestDto.getProductId())
                .orElseThrow(() -> {
                    logger.error("Product with ID {} not found or is deleted", requestDto.getProductId());
                    return new ResourceNotFoundException("Product with ID " + requestDto.getProductId() + " not found or is deleted");
                });

        Company company = companyRepository.findByIdAndIsDeletedFalse(requestDto.getCompanyId())
                .orElseThrow(() -> {
                    logger.error("Company with ID {} not found or is deleted", requestDto.getCompanyId());
                    return new ResourceNotFoundException("Company with ID " + requestDto.getCompanyId() + " not found or is deleted");
                });

        Contact contact = contactRepository.findByIdAndDeleteStatusFalse(requestDto.getContactId())
                .orElseThrow(() -> {
                    logger.error("Contact with ID {} not found or is deleted", requestDto.getContactId());
                    return new ResourceNotFoundException("Contact with ID " + requestDto.getContactId() + " not found or is deleted");
                });

        User updatedBy = userRepository.findByIdAndIsDeletedFalse(requestDto.getUpdatedBy())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", requestDto.getUpdatedBy());
                    return new ResourceNotFoundException("User with ID " + requestDto.getUpdatedBy() + " not found or is deleted");
                });

        User approvedBy = userRepository.findByIdAndIsDeletedFalse(requestDto.getApprovedById())
                .orElseThrow(() -> {
                    logger.error("Approved by user with ID {} not found or is deleted", requestDto.getApprovedById());
                    return new ResourceNotFoundException("Approved by user with ID " + requestDto.getApprovedById() + " not found or is deleted");
                });

        PaymentType paymentType = paymentTypeRepository.findById(requestDto.getPaymentTypeId())
                .orElseThrow(() -> {
                    logger.error("Payment type with ID {} not found", requestDto.getPaymentTypeId());
                    return new ResourceNotFoundException("Payment type with ID " + requestDto.getPaymentTypeId() + " not found");
                });

        double totalAmount = requestDto.getTotalAmount();
        double paidAmount = requestDto.getPaidAmount() != null ? requestDto.getPaidAmount() : 0.0;
        double dueAmount = totalAmount - paidAmount;

        mapRequestDtoToProject(project, requestDto);
        project.setSalesPerson(salesPerson);
        project.setProduct(product);
        project.setCompany(company);
        project.setContact(contact);
        project.setUpdatedBy(updatedBy.getId());
        project.setUpdatedDate(new Date());

        ProjectPaymentDetail paymentDetail = project.getPaymentDetail();
        paymentDetail.setTotalAmount(totalAmount);
        paymentDetail.setDueAmount(dueAmount);
        paymentDetail.setPaymentStatus(requestDto.getPaymentStatus());
        paymentDetail.setPaymentType(paymentType);
        paymentDetail.setApprovedBy(approvedBy);
        paymentDetail.setUpdatedBy(updatedBy.getId());
        paymentDetail.setUpdatedDate(new Date());

        project = projectRepository.save(project);
        logger.info("Project updated successfully with ID: {}", id);

        // Update milestone visibilities based on new payment
        updateMilestoneVisibilities(project, updatedBy.getId());

        return mapToResponseDto(project);
    }

    @Override
    public void deleteProject(Long id) {
        logger.info("Deleting project with ID: {}", id);
        Project project = projectRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.error("Project with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Project with ID " + id + " not found or is deleted");
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

        Project project = projectRepository.findByIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> {
                    logger.error("Project with ID {} not found or is deleted", projectId);
                    return new ResourceNotFoundException("Project with ID " + projectId + " not found or is deleted");
                });

        ProjectPaymentDetail paymentDetail = projectPaymentDetailRepository.findByProjectIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> {
                    logger.error("Payment detail for project ID {} not found or is deleted", projectId);
                    return new ResourceNotFoundException("Payment detail for project ID " + projectId + " not found or is deleted");
                });

        double amount = transactionDto.getAmount();

        // Validate payment amount
        if (amount <= 0) {
            logger.warn("Invalid payment amount: {}", amount);
            throw new ValidationException("Payment amount must be positive");
        }
        if (paymentDetail.getPaymentType().getName().equals("FULL") && paymentDetail.getDueAmount() == 0) {
            logger.warn("Attempt to add payment to fully paid project ID: {}", projectId);
            throw new ValidationException("Cannot add payment to a fully paid project");
        }
        if (amount > paymentDetail.getDueAmount()) {
            logger.warn("Payment amount {} exceeds due amount {}", amount, paymentDetail.getDueAmount());
            throw new ValidationException("Payment amount cannot exceed due amount of " + paymentDetail.getDueAmount());
        }

        // Update due amount
        paymentDetail.setDueAmount(paymentDetail.getDueAmount() - amount);

        User createdBy = userRepository.findByIdAndIsDeletedFalse(transactionDto.getCreatedBy())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", transactionDto.getCreatedBy());
                    return new ResourceNotFoundException("User with ID " + transactionDto.getCreatedBy() + " not found or is deleted");
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

    public void updateMilestoneStatus(Long assignmentId, MilestoneStatus newStatus, String statusReason, Long changedById) {
        logger.info("Updating milestone assignment ID: {} to status: {}", assignmentId, newStatus);
        ProjectMilestoneAssignment assignment = projectMilestoneAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId)
                .orElseThrow(() -> {
                    logger.error("Milestone assignment with ID {} not found or is deleted", assignmentId);
                    return new ResourceNotFoundException("Milestone assignment with ID " + assignmentId + " not found or is deleted");
                });

        User changedBy = userRepository.findByIdAndIsDeletedFalse(changedById)
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", changedById);
                    return new ResourceNotFoundException("User with ID " + changedById + " not found or is deleted");
                });

        // Validate status transition
        validateMilestoneStatusTransition(assignment, newStatus, statusReason);

        // Validate documents for COMPLETED status
        if (newStatus == MilestoneStatus.COMPLETED) {
            List<ProjectDocumentUpload> documents = projectDocumentUploadRepository.findByMilestoneAssignmentIdAndIsDeletedFalse(assignmentId);
            if (!assignment.getProductMilestoneMap().isAutoGenerated() && !documents.isEmpty()) {
                boolean allVerified = documents.stream().allMatch(doc -> doc.getStatus() == DocumentStatus.VERIFIED);
                if (!allVerified) {
                    logger.warn("Cannot complete milestone ID: {} due to unverified documents", assignmentId);
                    throw new ValidationException("All documents must be verified to complete milestone");
                }
            }
        }

        // Handle REJECTED → NEW for rework
        if (newStatus == MilestoneStatus.NEW && assignment.getStatus() == MilestoneStatus.REJECTED) {
            ProductMilestoneMap map = assignment.getProductMilestoneMap();
            if (!map.isAllowRollback()) {
                logger.warn("Rollback not allowed for milestone: {}", map.getMilestone().getName());
                throw new ValidationException("Rollback not allowed for milestone " + map.getMilestone().getName());
            }
            if (assignment.getReworkAttempts() >= map.getMaxAttempts()) {
                logger.warn("Maximum rework attempts ({}) reached for milestone ID: {}", map.getMaxAttempts(), assignmentId);
                throw new ValidationException("Maximum rework attempts reached for milestone");
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
        if (newStatus == MilestoneStatus.IN_PROGRESS) {
            assignment.setStartedDate(new Date());
        } else if (newStatus == MilestoneStatus.COMPLETED) {
            assignment.setCompletedDate(new Date());
        } else if (newStatus == MilestoneStatus.REJECTED || newStatus == MilestoneStatus.ON_HOLD) {
            assignment.setStatusReason(statusReason);
        }
        assignment.setUpdatedBy(changedById);
        assignment.setUpdatedDate(new Date());

        projectMilestoneAssignmentRepository.save(assignment);
        milestoneStatusHistoryRepository.save(history);
        logger.info("Milestone assignment ID: {} updated to status: {}", assignmentId, newStatus);

        // Update project status
        updateProjectStatus(assignment.getProject(), changedById);

        // If completed, re-check visibilities to unlock/assign next
        if (newStatus == MilestoneStatus.COMPLETED) {
            updateMilestoneVisibilities(assignment.getProject(), changedById);
        }
    }

    private void updateMilestoneVisibilities(Project project, Long updatedById) {
        logger.debug("Updating milestone visibilities for project ID: {}", project.getId());
        double totalAmount = project.getPaymentDetail().getTotalAmount();
        double paidAmount = totalAmount - project.getPaymentDetail().getDueAmount();
        double paidPercentage = (paidAmount / totalAmount) * 100.0;

        List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());
        if (assignments.isEmpty()) {
            logger.warn("No milestone assignments found for project ID: {}", project.getId());
            return;
        }

        assignments.sort(Comparator.comparing(a -> a.getProductMilestoneMap().getOrder()));

        double cumulativePaymentPercentage = 0.0;
        for (ProjectMilestoneAssignment assignment : assignments) {
            ProductMilestoneMap map = assignment.getProductMilestoneMap();
            cumulativePaymentPercentage += map.getPaymentPercentage();
            boolean allPreviousCompleted = true;

            // Check if all previous milestones are COMPLETED
            for (ProjectMilestoneAssignment prevAssignment : assignments) {
                if (prevAssignment.getProductMilestoneMap().getOrder() < map.getOrder()) {
                    if (prevAssignment.getStatus() != MilestoneStatus.COMPLETED) {
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

            // Always update if isVisible differs or reason differs
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

                        // Save assignment history
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

                        // Update user project count
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

    private void updateProjectStatus(Project project, Long updatedById) {
        logger.debug("Updating project status for project ID: {}", project.getId());
        List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());

        if (assignments.isEmpty()) {
            project.setStatus(ProjectStatus.OPEN);
        } else if (assignments.stream().allMatch(a -> a.getStatus() == MilestoneStatus.COMPLETED)) {
            project.setStatus(ProjectStatus.COMPLETED);
        } else if (assignments.stream().anyMatch(a -> a.getStatus() == MilestoneStatus.IN_PROGRESS || a.getStatus() == MilestoneStatus.ON_HOLD)) {
            project.setStatus(ProjectStatus.IN_PROGRESS);
        } else {
            project.setStatus(ProjectStatus.OPEN);
        }

        project.setUpdatedBy(updatedById);
        project.setUpdatedDate(new Date());
        projectRepository.save(project);
        logger.debug("Project ID: {} status updated to: {}", project.getId(), project.getStatus());
    }

    private void validateMilestoneStatusTransition(ProjectMilestoneAssignment assignment, MilestoneStatus newStatus, String statusReason) {
        if (statusReason == null || statusReason.trim().isEmpty()) {
            if (newStatus == MilestoneStatus.COMPLETED || newStatus == MilestoneStatus.ON_HOLD || newStatus == MilestoneStatus.REJECTED) {
                logger.warn("Status reason is required for status: {}", newStatus);
                throw new ValidationException("Status reason is required for status: " + newStatus);
            }
        }

        MilestoneStatus currentStatus = assignment.getStatus();
        if (currentStatus == newStatus) {
            logger.warn("Milestone assignment ID: {} already in status: {}", assignment.getId(), newStatus);
            throw new ValidationException("Milestone is already in status: " + newStatus);
        }

        switch (currentStatus) {
            case NEW:
                if (newStatus != MilestoneStatus.IN_PROGRESS && newStatus != MilestoneStatus.ON_HOLD) {
                    throw new ValidationException("Invalid transition from NEW to " + newStatus);
                }
                break;
            case IN_PROGRESS:
                if (newStatus != MilestoneStatus.COMPLETED && newStatus != MilestoneStatus.ON_HOLD && newStatus != MilestoneStatus.REJECTED) {
                    throw new ValidationException("Invalid transition from IN_PROGRESS to " + newStatus);
                }
                break;
            case ON_HOLD:
                if (newStatus != MilestoneStatus.IN_PROGRESS) {
                    throw new ValidationException("Invalid transition from ON_HOLD to " + newStatus);
                }
                break;
            case REJECTED:
                if (newStatus != MilestoneStatus.NEW) {
                    throw new ValidationException("Invalid transition from REJECTED to " + newStatus);
                }
                break;
            case COMPLETED:
                throw new ValidationException("Cannot change status from COMPLETED");
            default:
                throw new ValidationException("Invalid current status: " + currentStatus);
        }

        if (!assignment.isVisible() && newStatus != MilestoneStatus.NEW) {
            logger.warn("Cannot update milestone ID: {} to {} when not visible", assignment.getId(), newStatus);
            throw new ValidationException("Milestone must be visible to change status to " + newStatus);
        }
    }

    private void validateTransactionDto(ProjectPaymentTransactionDto transactionDto) {
        if (transactionDto.getAmount() == null) {
            logger.warn("Transaction amount is null");
            throw new ValidationException("Transaction amount cannot be null");
        }
        if (transactionDto.getPaymentDate() == null) {
            logger.warn("Transaction date is null");
            throw new ValidationException("Transaction date cannot be null");
        }
        if (transactionDto.getCreatedBy() == null) {
            logger.warn("Created by user ID is null");
            throw new ValidationException("Created by user ID cannot be null");
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
            throw new ResourceNotFoundException("No departments associated with milestone " + milestone.getMilestone().getName());
        }

        Department primaryDepartment = milestoneDepartments.get(0);
        logger.debug("Primary department for milestone {}: {} (ID: {})",
                milestone.getMilestone().getName(), primaryDepartment.getName(), primaryDepartment.getId());

        List<User> departmentUsers = userRepository.findByDepartmentsIdAndIsDeletedFalse(primaryDepartment.getId());
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
                .filter(m -> {
                    Optional<UserLoginStatus> status = userOnlineStatusRepository.findByUserIdAndIsDeletedFalse(m.getUser().getId());
                    return status.isPresent() && status.get().isOnline();
                })
                .max(Comparator.comparingDouble(m -> m.getRating() != null ? m.getRating() : 0.0));

        if (selectedMappingOpt.isPresent()) {
            UserProductMap selectedMapping = selectedMappingOpt.get();
            User selectedUser = selectedMapping.getUser();
            selectedMapping.setAssigned(true);
            userProductMapRepository.save(selectedMapping);
            logger.info("Assigned user: {} (ID: {}, Rating: {}) for milestone: {}",
                    selectedUser.getFullName(), selectedUser.getId(), selectedMapping.getRating(), milestone.getMilestone().getName());
            return new AssignmentResult(selectedUser, "Highest rating in round-robin and online");
        }

        Optional<User> selectedManagerOpt = eligibleMappings.stream()
                .filter(m -> !m.isAssigned())
                .filter(m -> m.getUser().getManager() != null)
                .filter(m -> {
                    Optional<UserLoginStatus> status = userOnlineStatusRepository.findByUserIdAndIsDeletedFalse(m.getUser().getManager().getId());
                    return status.isPresent() && status.get().isOnline() && isUserAvailable(m.getUser().getManager());
                })
                .map(m -> m.getUser().getManager())
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
            logger.info("Assigned manager: {} (ID: {}) for milestone: {} due to user offline",
                    manager.getFullName(), manager.getId(), milestone.getMilestone().getName());
            return new AssignmentResult(manager, "Manager assigned due to user offline");
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
            return new AssignmentResult(admin, "Admin assigned due to no online users or managers");
        }

        logger.error("No available admins found for milestone: {}", milestone.getMilestone().getName());
        throw new ResourceNotFoundException("No available users, managers, or admins found for milestone " + milestone.getMilestone().getName());
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
        dto.setPaymentStatus(project.getPaymentDetail() != null ? project.getPaymentDetail().getPaymentStatus() : null);
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
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found or is deleted"));

        PageRequest pageable = PageRequest.of(page, size * 10); // Adjust size to account for multiple assignments per project
        Page<ProjectMilestoneAssignment> assignmentPage;
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        if (isAdmin) {
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
                    deptUserIds, Arrays.asList(MilestoneStatus.NEW, MilestoneStatus.IN_PROGRESS), pageable);
        } else {
            assignmentPage = projectMilestoneAssignmentRepository.findByAssignedUserIdAndIsVisibleTrueAndStatusIn(
                    userId, Arrays.asList(MilestoneStatus.NEW, MilestoneStatus.IN_PROGRESS), pageable);
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
            dto.setProject(mapToProjectDetailsDto(entry.getKey()));
//            dto.setAssignedMilestones(entry.getValue().stream()
//                    .map(this::mapToAssignedMilestoneDto)
//                    .collect(Collectors.toList()));
            projectDtos.add(dto);
        }

        int start = Math.min(page * size, projectDtos.size());
        int end = Math.min(start + size, projectDtos.size());
        List<AssignedProjectResponseDto> pagedDtos = projectDtos.subList(start, end);
        return new PageImpl<>(pagedDtos, PageRequest.of(page, size), projectDtos.size());
    }

    private ProjectDetailsDto mapToProjectDetailsDto(Project project) {
        ProjectDetailsDto dto = new ProjectDetailsDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setProjectNo(project.getProjectNo());
        dto.setDate(project.getDate());
        dto.setAddress(project.getAddress());
        dto.setCity(project.getCity());
        dto.setState(project.getState());
        dto.setCountry(project.getCountry());
        dto.setCreatedDate(project.getCreatedDate());
        dto.setUpdatedDate(project.getUpdatedDate());
        return dto;
    }

    private AssignedMilestoneDto mapToAssignedMilestoneDto(ProjectMilestoneAssignment assignment) {
        AssignedMilestoneDto dto = new AssignedMilestoneDto();
        dto.setId(assignment.getId());
        dto.setProjectId(assignment.getProject().getId());
        dto.setProjectName(assignment.getProject().getName());
        dto.setMilestoneId(assignment.getMilestone().getId());
        dto.setMilestoneName(assignment.getMilestone().getName());
        dto.setStatus(assignment.getStatus().name());
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
        dto.setStatus(document.getStatus());
        dto.setRemarks(document.getRemarks());
        dto.setUploadTime(document.getUploadTime());
        dto.setRequiredDocumentId(document.getRequiredDocument().getUuid());
        dto.setMilestoneAssignmentId(document.getMilestoneAssignment().getId());
        dto.setProjectId(document.getProject().getId());
        dto.setUploadedById(document.getUploadedBy().getId());
        dto.setCreatedDate(document.getCreatedDate());
        dto.setUpdatedDate(document.getUpdatedDate());
        return dto;
    }

    @Override
    public List<AssignedMilestoneDto> getProjectMilestones(Long projectId, Long userId) {
        logger.info("Fetching milestones for project ID: {}, user ID: {}", projectId, userId);
        Project project = projectRepository.findByIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> {
                    logger.error("Project with ID {} not found or is deleted", projectId);
                    return new ResourceNotFoundException("Project with ID " + projectId + " not found or is deleted");
                });

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found or is deleted");
                });

        List<ProjectMilestoneAssignment> assignments;
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        if (isAdmin) {
            assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(projectId);
        } else if (user.isManagerFlag()) {
            List<Department> managerDepts = user.getDepartments();
            if (managerDepts.isEmpty()) {
                return new ArrayList<>();
            }
            List<Long> deptIds = managerDepts.stream().map(Department::getId).collect(Collectors.toList());
            List<User> deptUsers = userRepository.findByDepartmentIdsIn(deptIds);
            List<Long> deptUserIds = deptUsers.stream().map(User::getId).collect(Collectors.toList());
            if (!deptUserIds.contains(userId)) {
                deptUserIds.add(userId);
            }
            assignments = projectMilestoneAssignmentRepository.findByProjectIdAndAssignedUserIdInAndIsVisibleTrueAndStatusIn(
                    projectId, deptUserIds, Arrays.asList(MilestoneStatus.NEW, MilestoneStatus.IN_PROGRESS));
        } else {
            assignments = projectMilestoneAssignmentRepository.findByProjectIdAndAssignedUserIdAndIsVisibleTrueAndStatusIn(
                    projectId, userId, Arrays.asList(MilestoneStatus.NEW, MilestoneStatus.IN_PROGRESS));
        }

        for (ProjectMilestoneAssignment assignment : assignments) {
            assignment.setDocuments(projectDocumentUploadRepository.findByMilestoneAssignmentIdAndIsDeletedFalse(assignment.getId()));
        }

        return assignments.stream()
                .map(this::mapToAssignedMilestoneDto)
                .collect(Collectors.toList());
    }
}