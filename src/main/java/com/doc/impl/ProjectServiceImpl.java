package com.doc.impl;

import com.doc.dto.project.ProjectRequestDto;
import com.doc.dto.project.ProjectResponseDto;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.Contact;
import com.doc.entity.client.PaymentType;
import com.doc.entity.project.*;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.user.Department;
import com.doc.entity.user.User;
import com.doc.entity.user.UserProductMap;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.*;
import com.doc.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
        validateRequestDto(requestDto);

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
        List<ProductMilestoneMap> milestones = productMilestoneMapRepository.findByProductId(product.getId(), PageRequest.of(0, Integer.MAX_VALUE)).getContent();
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
        paymentDetail.setDeleted(false);

        project.setPaymentDetail(paymentDetail);

        project = projectRepository.save(project);
        logger.debug("Project saved with ID: {}", project.getId());

        // Record initial payment transaction if any payment was made
        if (paidAmount > 0) {
            ProjectPaymentTransaction transaction = new ProjectPaymentTransaction();
            transaction.setProject(project);
            transaction.setAmount(paidAmount);
            transaction.setPaymentDate(new Date());
            transaction.setCreatedBy(createdBy.getId());
            transaction.setCreatedDate(new Date());
            projectPaymentTransactionRepository.save(transaction);
            logger.debug("Initial payment transaction recorded for project ID: {}, amount: {}", project.getId(), paidAmount);
        }

        // Create milestone assignments and assign users immediately
        for (ProductMilestoneMap milestone : milestones) {
            ProjectMilestoneAssignment assignment = new ProjectMilestoneAssignment();
            assignment.setProject(project);
            assignment.setProductMilestoneMap(milestone);
            assignment.setMilestone(milestone.getMilestone());
            assignment.setStatus("LOCKED");
            assignment.setCreatedBy(createdBy.getId());
            assignment.setUpdatedBy(updatedBy.getId());
            assignment.setCreatedDate(new Date());
            assignment.setUpdatedDate(new Date());
            assignment.setDate(LocalDate.now());
            assignment.setDeleted(false);

            AssignmentResult assignmentResult = null;
            if (!milestone.isAutoGenerated()) {
                try {
                    assignmentResult = assignMilestoneUser(milestone);
                    if (assignmentResult != null) {
                        assignment.setAssignedUser(assignmentResult.user);
                        logger.debug("Assigned user {} (ID: {}) to milestone {} for project ID: {}",
                                assignmentResult.user.getFullName(), assignmentResult.user.getId(),
                                milestone.getMilestone().getName(), project.getId());
                    }
                } catch (ResourceNotFoundException e) {
                    logger.error("Failed to assign user to milestone {}: {}", milestone.getMilestone().getName(), e.getMessage());
                    throw e;
                }
            } else {
                logger.debug("Milestone {} is auto-generated, no user assignment needed", milestone.getMilestone().getName());
            }

            ProjectMilestoneAssignment savedAssignment = projectMilestoneAssignmentRepository.save(assignment);

            if (assignmentResult != null && assignmentResult.user != null) {
                // Save assignment history
                ProjectAssignmentHistory history = new ProjectAssignmentHistory();
                history.setProject(project);
                history.setMilestoneAssignment(savedAssignment);
                history.setAssignedUser(assignmentResult.user);
                history.setAssignmentReason(assignmentResult.reason);
                history.setCreatedDate(new Date());
                history.setUpdatedDate(new Date());
                history.setCreatedBy(createdBy.getId());
                history.setUpdatedBy(updatedBy.getId());
                history.setDeleted(false);
                projectAssignmentHistoryRepository.save(history);

                // Update user project count
                UserProjectCount count = userProjectCountRepository.findByUserIdAndProductId(assignmentResult.user.getId(), product.getId());
                if (count == null) {
                    count = new UserProjectCount();
                    count.setUser(assignmentResult.user);
                    count.setProduct(product);
                    count.setProjectCount(1);
                    count.setLastUpdatedDate(new Date());
                    count.setCreatedDate(new Date());
                    count.setUpdatedDate(new Date());
                    count.setCreatedBy(createdBy.getId());
                    count.setUpdatedBy(updatedBy.getId());
                    count.setDeleted(false);
                } else {
                    count.setProjectCount(count.getProjectCount() + 1);
                    count.setLastUpdatedDate(new Date());
                    count.setUpdatedDate(new Date());
                    count.setUpdatedBy(updatedBy.getId());
                }
                userProjectCountRepository.save(count);
            }

            logger.debug("Milestone assignment created for project ID: {}, milestone: {}", project.getId(), milestone.getMilestone().getName());
        }

        // Update milestone statuses based on initial payment (unlock if thresholds met)
        updateMilestoneStatuses(project, createdBy.getId());

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
    public List<ProjectResponseDto> getAllProjects(int page, int size) {
        logger.info("Fetching all projects, page: {}, size: {}", page, size);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Project> projectPage = projectRepository.findByIsDeletedFalse(pageable);
        return projectPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectResponseDto updateProject(Long id, ProjectRequestDto requestDto) {
        logger.info("Updating project with ID: {}", id);
        validateRequestDto(requestDto);

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

        // Update milestone statuses based on new payment
        updateMilestoneStatuses(project, updatedBy.getId());

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
        projectRepository.save(project);
        logger.info("Project deleted successfully with ID: {}", id);
    }

    @Override
    public ProjectResponseDto addPaymentTransaction(Long projectId, ProjectPaymentTransactionDto transactionDto) {
        logger.info("Adding payment transaction for project ID: {}", projectId);
        if (transactionDto.getAmount() == null || transactionDto.getAmount() <= 0) {
            logger.warn("Invalid payment amount: {}", transactionDto.getAmount());
            throw new ValidationException("Payment amount must be positive");
        }
        if (transactionDto.getPaymentDate() == null) {
            logger.warn("Payment date is null");
            throw new ValidationException("Payment date cannot be null");
        }
        if (transactionDto.getCreatedBy() == null) {
            logger.warn("Created by user ID is null");
            throw new ValidationException("Created by user ID cannot be null");
        }

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

        if (paymentDetail.getPaymentType().getName().equals("FULL") && paymentDetail.getDueAmount() == 0) {
            logger.warn("Attempt to add payment to fully paid project ID: {}", projectId);
            throw new ValidationException("Cannot add payment to a fully paid project");
        }

        if (transactionDto.getAmount() > paymentDetail.getDueAmount()) {
            logger.warn("Payment amount {} exceeds due amount {}", transactionDto.getAmount(), paymentDetail.getDueAmount());
            throw new ValidationException("Payment amount cannot exceed due amount of " + paymentDetail.getDueAmount());
        }

        User createdBy = userRepository.findByIdAndIsDeletedFalse(transactionDto.getCreatedBy())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", transactionDto.getCreatedBy());
                    return new ResourceNotFoundException("User with ID " + transactionDto.getCreatedBy() + " not found or is deleted");
                });

        ProjectPaymentTransaction transaction = new ProjectPaymentTransaction();
        transaction.setProject(project);
        transaction.setAmount(transactionDto.getAmount());
        transaction.setPaymentDate(transactionDto.getPaymentDate());
        transaction.setCreatedBy(createdBy.getId());
        transaction.setCreatedDate(new Date());

        paymentDetail.setDueAmount(paymentDetail.getDueAmount() - transactionDto.getAmount());
        paymentDetail.setUpdatedBy(createdBy.getId());
        paymentDetail.setUpdatedDate(new Date());

        projectPaymentTransactionRepository.save(transaction);
        projectPaymentDetailRepository.save(paymentDetail);
        logger.info("Payment transaction added for project ID: {}, amount: {}", projectId, transactionDto.getAmount());

        // Update milestone statuses based on new payment
        updateMilestoneStatuses(project, createdBy.getId());

        return mapToResponseDto(project);
    }

    private void updateMilestoneStatuses(Project project, Long updatedById) {
        logger.debug("Updating milestone statuses for project ID: {}", project.getId());
        double totalAmount = project.getPaymentDetail().getTotalAmount();
        double paidAmount = totalAmount - project.getPaymentDetail().getDueAmount();
        double dueAmount = project.getPaymentDetail().getDueAmount();
        logger.debug("Project ID: {}, totalAmount: {}, paidAmount: {}, dueAmount: {}",
                project.getId(), totalAmount, paidAmount, dueAmount);

        List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());
        if (assignments.isEmpty()) {
            logger.warn("No milestone assignments found for project ID: {}", project.getId());
            return;
        }

        logger.debug("Found {} milestone assignments for project ID: {}", assignments.size(), project.getId());
        assignments.sort(Comparator.comparing(a -> a.getProductMilestoneMap().getOrder()));

        // Calculate cumulative payment percentage required for each milestone
        double cumulativePaymentPercentage = 0.0;
        for (ProjectMilestoneAssignment assignment : assignments) {
            ProductMilestoneMap map = assignment.getProductMilestoneMap();
            logger.debug("Evaluating milestone: {} (ID: {}, Order: {}) for project ID: {}",
                    map.getMilestone().getName(), map.getMilestone().getId(), map.getOrder(), project.getId());

            if ("LOCKED".equals(assignment.getStatus())) {
                // Special condition for Certification milestone
                if (map.getMilestone().getName().equalsIgnoreCase("Certification")) {
                    logger.debug("Certification milestone check: dueAmount = {}", dueAmount);
                    // Only unlock Certification if due_amount is 0
                    if (dueAmount == 0) {
                        assignment.setStatus("UNLOCKED");
                        assignment.setUpdatedBy(updatedById);
                        assignment.setUpdatedDate(new Date());

                        // Assign user if not assigned and not auto-generated
                        AssignmentResult assignmentResult = null;
                        if (assignment.getAssignedUser() == null && !map.isAutoGenerated()) {
                            try {
                                assignmentResult = assignMilestoneUser(map);
                                if (assignmentResult != null) {
                                    assignment.setAssignedUser(assignmentResult.user);
                                    logger.debug("Assigned user {} (ID: {}) to Certification milestone for project ID: {}",
                                            assignmentResult.user.getFullName(), assignmentResult.user.getId(), project.getId());
                                }
                            } catch (ResourceNotFoundException e) {
                                logger.error("Failed to assign user to Certification milestone: {}", e.getMessage());
                                throw e;
                            }
                        } else if (map.isAutoGenerated()) {
                            logger.debug("Certification milestone is auto-generated, no user assignment needed");
                        }

                        ProjectMilestoneAssignment savedAssignment = projectMilestoneAssignmentRepository.save(assignment);

                        if (assignmentResult != null && assignmentResult.user != null) {
                            // Save assignment history
                            ProjectAssignmentHistory history = new ProjectAssignmentHistory();
                            history.setProject(project);
                            history.setMilestoneAssignment(savedAssignment);
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

                        logger.info("Unlocked Certification milestone for project ID: {}, milestone: {}",
                                project.getId(), map.getMilestone().getName());
                    } else {
                        logger.debug("Certification milestone remains LOCKED, dueAmount: {}", dueAmount);
                    }
                } else {
                    // Add the current milestone's payment percentage to the cumulative total
                    cumulativePaymentPercentage += map.getPaymentPercentage();
                    double requiredPayment = totalAmount * (cumulativePaymentPercentage / 100.0);
                    logger.debug("Milestone: {}, cumulativePaymentPercentage: {}, requiredPayment: {}",
                            map.getMilestone().getName(), cumulativePaymentPercentage, requiredPayment);

                    // Check if all previous milestones are unlocked or completed
                    boolean allPreviousUnlocked = true;
                    for (ProjectMilestoneAssignment prevAssignment : assignments) {
                        if (prevAssignment.getProductMilestoneMap().getOrder() < map.getOrder()) {
                            if ("LOCKED".equals(prevAssignment.getStatus())) {
                                allPreviousUnlocked = false;
                                logger.debug("Previous milestone {} (Order: {}) is LOCKED, blocking milestone {}",
                                        prevAssignment.getMilestone().getName(),
                                        prevAssignment.getProductMilestoneMap().getOrder(),
                                        map.getMilestone().getName());
                                break;
                            }
                        }
                    }

                    // Unlock the milestone only if all previous milestones are unlocked and payment is sufficient
                    if (allPreviousUnlocked && paidAmount >= requiredPayment) {
                        assignment.setStatus("UNLOCKED");
                        assignment.setUpdatedBy(updatedById);
                        assignment.setUpdatedDate(new Date());

                        // Assign user if not assigned and not auto-generated
                        AssignmentResult assignmentResult = null;
                        if (assignment.getAssignedUser() == null && !map.isAutoGenerated()) {
                            try {
                                assignmentResult = assignMilestoneUser(map);
                                if (assignmentResult != null) {
                                    assignment.setAssignedUser(assignmentResult.user);
                                    logger.debug("Assigned user {} (ID: {}) to milestone {} for project ID: {}",
                                            assignmentResult.user.getFullName(), assignmentResult.user.getId(),
                                            map.getMilestone().getName(), project.getId());
                                }
                            } catch (ResourceNotFoundException e) {
                                logger.error("Failed to assign user to milestone {}: {}",
                                        map.getMilestone().getName(), e.getMessage());
                                throw e;
                            }
                        } else if (map.isAutoGenerated()) {
                            logger.debug("Milestone {} is auto-generated, no user assignment needed",
                                    map.getMilestone().getName());
                        }

                        ProjectMilestoneAssignment savedAssignment = projectMilestoneAssignmentRepository.save(assignment);

                        if (assignmentResult != null && assignmentResult.user != null) {
                            // Save assignment history
                            ProjectAssignmentHistory history = new ProjectAssignmentHistory();
                            history.setProject(project);
                            history.setMilestoneAssignment(savedAssignment);
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

                        logger.info("Unlocked milestone for project ID: {}, milestone: {}, cumulative payment: {}",
                                project.getId(), map.getMilestone().getName(), requiredPayment);
                    } else {
                        logger.debug("Milestone {} remains LOCKED. allPreviousUnlocked: {}, paidAmount: {}, requiredPayment: {}",
                                map.getMilestone().getName(), allPreviousUnlocked, paidAmount, requiredPayment);
                    }
                }
            } else {
                logger.debug("Milestone {} (ID: {}) is already {}, skipping",
                        map.getMilestone().getName(), map.getMilestone().getId(), assignment.getStatus());
            }
        }

        // Warn if no milestones were unlocked
        if (assignments.stream().noneMatch(a -> "UNLOCKED".equals(a.getStatus()))) {
            logger.warn("No milestones unlocked for project ID: {} after payment. paidAmount: {}, totalAmount: {}",
                    project.getId(), paidAmount, totalAmount);
        }
    }

    private AssignmentResult assignMilestoneUser(ProductMilestoneMap milestone) {
        logger.info("Assigning user for milestone: {}, product ID: {}", milestone.getMilestone().getName(), milestone.getProduct().getId());

        // Skip assignment for auto-generated milestones (e.g., Certification)
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
        logger.debug("Found {} users in department {} (ID: {})", departmentUsers.size(), primaryDepartment.getName(), primaryDepartment.getId());
        if (departmentUsers.isEmpty()) {
            logger.warn("No users found in department {} (ID: {}) for milestone: {}",
                    primaryDepartment.getName(), primaryDepartment.getId(), milestone.getMilestone().getName());
            User admin = assignAdmin(milestone);
            return new AssignmentResult(admin, "Admin assigned");
        } else {
            logger.debug("Users in department {} (ID: {}): {}",
                    primaryDepartment.getName(), primaryDepartment.getId(),
                    departmentUsers.stream()
                            .map(u -> String.format("%s (ID: %d, Deleted: %b)", u.getFullName(), u.getId(), u.isDeleted()))
                            .collect(Collectors.joining(", ")));
        }

        List<UserProductMap> mappings = userProductMapRepository.findByProductIdAndIsDeletedFalse(milestone.getProduct().getId());
        logger.debug("Found {} UserProductMap entries for product ID: {}", mappings.size(), milestone.getProduct().getId());
        if (!mappings.isEmpty()) {
            logger.debug("UserProductMap entries for product ID: {}: {}",
                    milestone.getProduct().getId(),
                    mappings.stream()
                            .map(m -> String.format("User: %s (ID: %d, Rating: %s, Assigned: %b, Deleted: %b)",
                                    m.getUser().getFullName(), m.getUser().getId(),
                                    m.getRating() != null ? m.getRating() : "null", m.isAssigned(), m.getUser().isDeleted()))
                            .collect(Collectors.joining(", ")));
        }

        // Filter department users who have a UserProductMap entry for the product
        List<UserProductMap> eligibleMappings = mappings.stream()
                .filter(m -> departmentUsers.stream().anyMatch(u -> u.getId().equals(m.getUser().getId())))
                .filter(m -> m.getRating() != null && m.getRating() > 0)
                .filter(m -> isUserAvailable(m.getUser()))
                .collect(Collectors.toList());

        if (eligibleMappings.isEmpty()) {
            logger.warn("No eligible users with positive ratings found for product ID: {} in department: {}",
                    milestone.getProduct().getId(), primaryDepartment.getName());
            User admin = assignAdmin(milestone);
            return new AssignmentResult(admin, "Admin assigned");
        }

        // Check if all eligible users are assigned
        boolean allAssigned = eligibleMappings.stream().allMatch(UserProductMap::isAssigned);
        if (allAssigned) {
            logger.debug("All eligible users for product ID: {} are assigned, resetting isAssigned flags", milestone.getProduct().getId());
            eligibleMappings.forEach(m -> {
                m.setAssigned(false);
                userProductMapRepository.save(m);
            });
        }

        // Select the highest-rated unassigned user
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
            return new AssignmentResult(selectedUser, "Highest rating in round-robin");
        }

        // Fallback to manager if no unassigned users (shouldn't happen after reset)
        Optional<User> bestUserOpt = departmentUsers.stream()
                .filter(this::isUserAvailable)
                .max(Comparator.comparingDouble((User u) -> mappings.stream()
                                .filter(m -> m.getUser().getId().equals(u.getId()))
                                .findFirst()
                                .map(m -> m.getRating() != null ? m.getRating() : 0.0)
                                .orElse(0.0))
                        .thenComparingLong(User::getId));

        if (bestUserOpt.isPresent()) {
            User bestUser = bestUserOpt.get();
            User manager = bestUser.getManager();
            if (manager != null && isUserAvailable(manager)) {
                logger.info("No unassigned users with positive rating found, assigned manager: {} (ID: {}) for milestone: {}",
                        manager.getFullName(), manager.getId(), milestone.getMilestone().getName());
                return new AssignmentResult(manager, "Manager fallback");
            }
        }

        User admin = assignAdmin(milestone);
        return new AssignmentResult(admin, "Admin assigned");
    }

    private User assignAdmin(ProductMilestoneMap milestone) {
        List<User> admins = userRepository.findAdmins();
        logger.debug("Found {} admins for milestone: {}", admins.size(), milestone.getMilestone().getName());
        if (!admins.isEmpty()) {
            logger.debug("Admins: {}",
                    admins.stream()
                            .map(a -> String.format("%s (ID: %d, Deleted: %b)", a.getFullName(), a.getId(), a.isDeleted()))
                            .collect(Collectors.joining(", ")));
        }

        Optional<User> availableAdmin = admins.stream()
                .filter(this::isUserAvailable)
                .findFirst();

        if (availableAdmin.isPresent()) {
            logger.info("Assigned admin: {} (ID: {}) for milestone: {}",
                    availableAdmin.get().getFullName(), availableAdmin.get().getId(), milestone.getMilestone().getName());
            return availableAdmin.get();
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

    private void validateRequestDto(ProjectRequestDto requestDto) {
        logger.debug("Validating project request DTO: {}", requestDto.getProjectNo());
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            logger.warn("Project name is empty");
            throw new ValidationException("Project name cannot be empty");
        }
        if (requestDto.getProjectNo() == null || requestDto.getProjectNo().trim().isEmpty()) {
            logger.warn("Project number is empty");
            throw new ValidationException("Project number cannot be empty");
        }
        if (requestDto.getSalesPersonId() == null) {
            logger.warn("Sales person ID is null");
            throw new ValidationException("Sales person ID cannot be null");
        }
        if (requestDto.getTotalAmount() == null || requestDto.getTotalAmount() < 0) {
            logger.warn("Invalid total amount: {}", requestDto.getTotalAmount());
            throw new ValidationException("Total amount must be non-negative");
        }
        if (requestDto.getPaidAmount() == null || requestDto.getPaidAmount() < 0) {
            logger.warn("Invalid paid amount: {}", requestDto.getPaidAmount());
            throw new ValidationException("Paid amount must be non-negative");
        }
        if (requestDto.getPaidAmount() > requestDto.getTotalAmount()) {
            logger.warn("Paid amount {} exceeds total amount {}", requestDto.getPaidAmount(), requestDto.getTotalAmount());
            throw new ValidationException("Paid amount cannot exceed total amount");
        }
        if (!List.of("PENDING", "APPROVED", "REJECTED").contains(requestDto.getPaymentStatus())) {
            logger.warn("Invalid payment status: {}", requestDto.getPaymentStatus());
            throw new ValidationException("Payment status must be PENDING, APPROVED, or REJECTED");
        }
        if (requestDto.getProductId() == null) {
            logger.warn("Product ID is null");
            throw new ValidationException("Product ID cannot be null");
        }
        if (requestDto.getCompanyId() == null) {
            logger.warn("Company ID is null");
            throw new ValidationException("Company ID cannot be null");
        }
        if (requestDto.getContactId() == null) {
            logger.warn("Contact ID is null");
            throw new ValidationException("Contact ID cannot be null");
        }
        if (requestDto.getPaymentTypeId() == null) {
            logger.warn("Payment type ID is null");
            throw new ValidationException("Payment type ID cannot be null");
        }
        if (requestDto.getApprovedById() == null) {
            logger.warn("Approved by user ID is null");
            throw new ValidationException("Approved by user ID cannot be null");
        }
        if (requestDto.getCreatedBy() == null) {
            logger.warn("Created by user ID is null");
            throw new ValidationException("Created by user ID cannot be null");
        }
        if (requestDto.getUpdatedBy() == null) {
            logger.warn("Updated by user ID is null");
            throw new ValidationException("Updated by user ID cannot be null");
        }
        logger.debug("Project request DTO validated successfully");
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
}