package com.doc.impl;

import com.doc.dto.project.ProjectRequestDto;
import com.doc.dto.project.ProjectResponseDto;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.Contact;
import com.doc.entity.client.PaymentType;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectPaymentDetail;
import com.doc.entity.project.ProjectPaymentTransaction;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.user.Department;
import com.doc.entity.user.User;
import com.doc.entity.user.UserProductMap;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repsoitory.*;
import com.doc.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



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

        double totalAmount = requestDto.getTotalAmount();
        double paidAmount = totalAmount - requestDto.getDueAmount();

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
        paymentDetail.setDueAmount(requestDto.getDueAmount());
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

        // Create milestone assignments (all initially LOCKED, no user assigned yet)
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
            assignment.setDeleted(false);

            projectMilestoneAssignmentRepository.save(assignment);
            logger.debug("Milestone assignment created for project ID: {}, milestone: {}", project.getId(), milestone.getMilestone().getName());
        }

        // Update milestone statuses based on initial payment (unlock and assign if thresholds met)
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

        mapRequestDtoToProject(project, requestDto);
        project.setSalesPerson(salesPerson);
        project.setProduct(product);
        project.setCompany(company);
        project.setContact(contact);
        project.setUpdatedBy(updatedBy.getId());
        project.setUpdatedDate(new Date());

        ProjectPaymentDetail paymentDetail = project.getPaymentDetail();
        paymentDetail.setTotalAmount(requestDto.getTotalAmount());
        paymentDetail.setDueAmount(requestDto.getDueAmount());
        paymentDetail.setPaymentStatus(requestDto.getPaymentStatus());
        paymentDetail.setPaymentType(paymentType);
        paymentDetail.setApprovedBy(approvedBy);
        paymentDetail.setUpdatedBy(updatedBy.getId());
        paymentDetail.setUpdatedDate(new Date());

        project = projectRepository.save(project);
        logger.info("Project updated successfully with ID: {}", id);
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

        // Update milestone statuses based on new payment (unlock and assign if thresholds met)
        updateMilestoneStatuses(project, createdBy.getId());

        return mapToResponseDto(project);
    }

    private void updateMilestoneStatuses(Project project, Long updatedById) {
        double totalAmount = project.getPaymentDetail().getTotalAmount();
        double paidAmount = totalAmount - project.getPaymentDetail().getDueAmount();

        List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());
        assignments.sort(Comparator.comparing(a -> a.getProductMilestoneMap().getOrder()));

        for (ProjectMilestoneAssignment assignment : assignments) {
            if ("LOCKED".equals(assignment.getStatus())) {
                ProductMilestoneMap map = assignment.getProductMilestoneMap();
                double requiredPayment = totalAmount * (map.getPaymentPercentage() / 100.0);
                if (paidAmount >= requiredPayment) {
                    assignment.setStatus("UNLOCKED");
                    assignment.setUpdatedBy(updatedById);
                    assignment.setUpdatedDate(new Date());

                    // Assign user if not assigned and not auto-generated
                    if (assignment.getAssignedUser() == null && !map.isAutoGenerated()) {
                        User assignedUser = assignMilestoneUser(map);
                        assignment.setAssignedUser(assignedUser);
                    }



                    projectMilestoneAssignmentRepository.save(assignment);
                    logger.debug("Unlocked milestone for project ID: {}, milestone: {}", project.getId(), map.getMilestone().getName());
                }
            }
        }
    }

    private User assignMilestoneUser(ProductMilestoneMap milestone) {
        logger.debug("Assigning user for milestone: {}, product ID: {}", milestone.getMilestone().getName(), milestone.getProduct().getId());
        List<Long> deptIds = milestone.getMilestone().getDepartments().stream()
                .map(Department::getId)
                .collect(Collectors.toList());

        List<UserProductMap> mappings = userProductMapRepository.findByProductIdAndIsDeletedFalse(milestone.getProduct().getId());

        Optional<UserProductMap> bestMapping = mappings.stream()
                .filter(m -> isUserAvailable(m.getUser()))
                .filter(m -> m.getUser().getDepartments().stream().anyMatch(d -> deptIds.contains(d.getId())))
                .max(Comparator.comparingDouble(m -> m.getRating() != null ? m.getRating() : Double.MIN_VALUE));

        if (bestMapping.isPresent()) {
            return bestMapping.get().getUser();
        }

        // Fallback to manager of the first eligible user
        if (!mappings.isEmpty()) {
            User firstUser = mappings.get(0).getUser();
            User manager = firstUser.getManager();
            if (manager != null && isUserAvailable(manager) && manager.getDepartments().stream().anyMatch(d -> deptIds.contains(d.getId()))) {
                logger.debug("Assigned manager: {} (ID: {}) for milestone: {}", manager.getFullName(), manager.getId(), milestone.getMilestone().getName());
                return manager;
            }
        }

        // Fallback to admin
        List<User> admins = userRepository.findAdmins();
        for (User admin : admins) {
            if (isUserAvailable(admin) && admin.getDepartments().stream().anyMatch(d -> deptIds.contains(d.getId()))) {
                logger.debug("Assigned admin: {} (ID: {}) for milestone: {}", admin.getFullName(), admin.getId(), milestone.getMilestone().getName());
                return admin;
            }
        }

        logger.error("No available users, managers, or admins found for milestone: {}, product ID: {}", milestone.getMilestone().getName(), milestone.getProduct().getId());
        throw new ResourceNotFoundException("No available users found for milestone " + milestone.getMilestone().getName());
    }

    private boolean isUserAvailable(User user) {
        return user != null && !user.isDeleted();
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
        if (requestDto.getDueAmount() == null || requestDto.getDueAmount() < 0) {
            logger.warn("Invalid due amount: {}", requestDto.getDueAmount());
            throw new ValidationException("Due amount must be non-negative");
        }
        if (requestDto.getDueAmount() > requestDto.getTotalAmount()) {
            logger.warn("Due amount {} exceeds total amount {}", requestDto.getDueAmount(), requestDto.getTotalAmount());
            throw new ValidationException("Due amount cannot exceed total amount");
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