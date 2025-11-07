package com.doc.impl;

import com.doc.constants.StatusConstants;
import com.doc.dto.contact.ContactDetailsDto;
import com.doc.dto.project.*;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.dto.user.UserResponseDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.Contact;
import com.doc.entity.client.PaymentType;
import com.doc.entity.department.Department;
import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.project.*;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.*;
import com.doc.repository.department.DepartmentAutoConfigRepository;
import com.doc.repository.documentRepo.DocumentStatusRepository;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.service.AutoAssignmentService;
import com.doc.service.ProjectService;
import com.doc.validator.request.ProjectRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private PaymentTypeRepository paymentTypeRepository;
    @Autowired private ProjectPaymentDetailRepository projectPaymentDetailRepository;
    @Autowired private ProjectPaymentTransactionRepository projectPaymentTransactionRepository;
    @Autowired private ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;
    @Autowired private ProjectAssignmentHistoryRepository projectAssignmentHistoryRepository;
    @Autowired private UserPerformanceCountRepository userPerformanceCountRepository;
    @Autowired private UserProductMapRepository userProductMapRepository;
    @Autowired private UserLoginStatusRepository userOnlineStatusRepository;
    @Autowired private ProductMilestoneMapRepository productMilestoneMapRepository;
    @Autowired private ProjectDocumentUploadRepository projectDocumentUploadRepository;
    @Autowired private MilestoneStatusHistoryRepository milestoneStatusHistoryRepository;
    @Autowired private MilestoneStatusRepository milestoneStatusRepository;
    @Autowired private DocumentStatusRepository documentStatusRepository;
    @Autowired private ProjectStatusRepository projectStatusRepository;
    @Autowired private DepartmentAutoConfigRepository departmentAutoConfigRepository;
    @Autowired private AutoAssignmentService autoAssignmentService;
    @Autowired private ProjectRequestValidator projectRequestValidator;

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

        // Fetch entities
        Product product = productRepository.findActiveUserById(requestDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found", "ERR_PRODUCT_NOT_FOUND"));
        Company company = companyRepository.findActiveUserById(requestDto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found", "ERR_COMPANY_NOT_FOUND"));
        Contact contact = contactRepository.findByIdAndDeleteStatusFalseAndIsActiveTrue(requestDto.getContactId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found", "ERR_CONTACT_NOT_FOUND"));
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
        project = projectRepository.save(project);

        if (paidAmount > 0) {
            ProjectPaymentTransaction transaction = new ProjectPaymentTransaction();
            transaction.setProject(project);
            transaction.setAmount(paidAmount);
            transaction.setTransactionDate(new Date());
            transaction.setCreatedBy(createdBy.getId());
            transaction.setCreatedDate(new Date());
            projectPaymentTransactionRepository.save(transaction);
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
    public Page<ProjectResponseDto> getAllProjects(Long userId, int page, int size) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        PageRequest pageable = PageRequest.of(page, size);
        Page<Project> projectPage;

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        if (isAdmin) {
            projectPage = projectRepository.findByIsDeletedFalse(pageable);
        } else {
            List<Long> userIds = new ArrayList<>(List.of(userId));
            if (user.isManagerFlag()) {
                List<User> subordinates = userRepository.findByManagerIdAndIsDeletedFalse(userId);
                userIds.addAll(subordinates.stream().map(User::getId).toList());
            }
            projectPage = projectRepository.findByAssignedUserIds(userIds, pageable);
        }

        // Return full Page with metadata + mapped DTOs
        return projectPage.map(this::mapToResponseDto);
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
    }

    @Override
    public ProjectResponseDto addPaymentTransaction(Long projectId, ProjectPaymentTransactionDto dto) {
        validateTransactionDto(dto);

        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));

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

        updateMilestoneVisibilities(project, createdBy.getId());
        return mapToResponseDto(project);
    }

    @Override
    public void updateMilestoneVisibilities(Project project, Long updatedById) {
        double totalAmount = project.getPaymentDetail().getTotalAmount();
        double paidAmount = totalAmount - project.getPaymentDetail().getDueAmount();
        double paidPercentage = totalAmount > 0 ? (paidAmount / totalAmount) * 100.0 : 0.0;
        String paymentTypeName = project.getPaymentDetail().getPaymentType().getName();

        List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(project.getId());
        if (assignments.isEmpty()) return;

        assignments.sort(Comparator.comparing(a -> a.getProductMilestoneMap().getOrder()));

        MilestoneStatus completedStatus = milestoneStatusRepository.findById(StatusConstants.MILESTONE_COMPLETED_ID).orElse(null);

        if ("Purchase Order Payment".equalsIgnoreCase(paymentTypeName)) {
            for (ProjectMilestoneAssignment assignment : assignments) {
                ProductMilestoneMap map = assignment.getProductMilestoneMap();
                boolean isCertification = "Certification".equalsIgnoreCase(map.getMilestone().getName());
                boolean allPriorCompleted = assignments.stream()
                        .filter(a -> a.getProductMilestoneMap().getOrder() < map.getOrder())
                        .allMatch(a -> completedStatus != null && completedStatus.equals(a.getStatus()));
                boolean isVisible = isCertification
                        ? allPriorCompleted && Math.abs(project.getPaymentDetail().getDueAmount()) < 0.01
                        : true;
                String reason = isVisible ? null : (allPriorCompleted ? "Full payment required" : "Prior milestones incomplete");
                updateVisibilityAndAutoAssign(assignment, isVisible, reason, map, project, updatedById);
            }
        } else {
            double cumulative = 0.0;
            for (ProjectMilestoneAssignment assignment : assignments) {
                ProductMilestoneMap map = assignment.getProductMilestoneMap();
                cumulative += map.getPaymentPercentage();

                boolean allPrevCompleted = assignments.stream()
                        .filter(a -> a.getProductMilestoneMap().getOrder() < map.getOrder())
                        .allMatch(a -> completedStatus != null && completedStatus.equals(a.getStatus()));

                boolean isCertification = "Certification".equalsIgnoreCase(map.getMilestone().getName());
                boolean isVisible;
                String reason;

                if (isCertification) {
                    isVisible = allPrevCompleted && Math.abs(project.getPaymentDetail().getDueAmount()) < 0.01;
                    reason = isVisible ? null : (allPrevCompleted ? "Full payment required" : "Prior incomplete");
                } else {
                    isVisible = allPrevCompleted && paidPercentage >= cumulative;
                    reason = !isVisible ? (allPrevCompleted ? "Insufficient payment" : "Previous incomplete") : null;
                }

                updateVisibilityAndAutoAssign(assignment, isVisible, reason, map, project, updatedById);
            }
        }
    }

    private void updateVisibilityAndAutoAssign(ProjectMilestoneAssignment assignment, boolean isVisible, String reason,
                                               ProductMilestoneMap map, Project project, Long updatedById) {
        if (assignment.isVisible() != isVisible || !Objects.equals(reason, assignment.getVisibilityReason())) {
            assignment.setVisible(isVisible);
            assignment.setVisibilityReason(reason);
            assignment.setVisibleDate(isVisible ? new Date() : null);
            assignment.setUpdatedBy(updatedById);
            assignment.setUpdatedDate(new Date());
            projectMilestoneAssignmentRepository.save(assignment);
        }

        if (isVisible && !map.isAutoGenerated() && assignment.getAssignedUser() == null) {
            AssignmentResult result = autoAssignmentService.assignMilestoneUser(map, project, updatedById);
            assignment.setAssignedUser(result != null ? result.getUser() : null);
            assignment.setStatusReason(result != null ? result.getReason() : "Auto-assign failed");
            projectMilestoneAssignmentRepository.save(assignment);

            ProjectAssignmentHistory history = new ProjectAssignmentHistory();
            history.setProject(project);
            history.setMilestoneAssignment(assignment);
            history.setAssignedUser(result != null ? result.getUser() : null);
            history.setAssignmentReason(result != null ? result.getReason() : "Auto-assign failed");
            history.setCreatedDate(new Date());
            history.setUpdatedDate(new Date());
            history.setCreatedBy(updatedById);
            history.setUpdatedBy(updatedById);
            history.setDeleted(false);
            projectAssignmentHistoryRepository.save(history);
        }
    }

    @Cacheable(value = "milestoneMaps", key = "#productId")
    public List<ProductMilestoneMap> getMilestoneMaps(Long productId) {
        return productMilestoneMapRepository.findByProductId(productId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }

    private void validateTransactionDto(ProjectPaymentTransactionDto dto) {
        if (dto.getAmount() == null || dto.getAmount() <= 0) throw new ValidationException("Invalid amount", "ERR_INVALID_PAYMENT_AMOUNT");
        if (dto.getPaymentDate() == null) throw new ValidationException("Payment date required", "ERR_NULL_PAYMENT_DATE");
        if (dto.getCreatedBy() == null) throw new ValidationException("CreatedBy required", "ERR_NULL_CREATED_BY");
    }

    private void mapRequestDtoToProject(Project project, ProjectRequestDto dto) {
        project.setName(dto.getName().trim());
        project.setProjectNo(dto.getProjectNo().trim());
        project.setLeadId(dto.getLeadId());
        project.setDate(dto.getDate());
        project.setAddress(dto.getAddress());
        project.setCity(dto.getCity());
        project.setState(dto.getState());
        project.setCountry(dto.getCountry());
        project.setPrimaryPinCode(dto.getPrimaryPinCode());
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
        dto.setUnbilledNumber(project.getUnbilledNumber());
        dto.setEstimateNumber(project.getEstimateNumber());
        dto.setSalesPersonId(project.getSalesPersonId());
        dto.setSalesPersonName(project.getSalesPersonName());
        dto.setStatusId(project.getStatus() != null ? project.getStatus().getId() : null);
        dto.setStatusName(project.getStatus() != null ? project.getStatus().getName() : null);
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
        assignments.forEach(a -> a.setDocuments(projectDocumentUploadRepository.findByMilestoneAssignmentIdAndIsDeletedFalse(a.getId())));

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
    public ProjectMilestoneResponseDto getProjectMilestones(Long projectId, Long userId) {
        logger.info("Fetching project details and milestones for project ID: {}, user ID: {}", projectId, userId);
        System.out.println("Fetching project details and milestones for project ID: " + projectId + ", user ID: " + userId);
        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        boolean isOperationHead = user.getRoles().stream().anyMatch(role -> role.getName().equals("OPERATION_HEAD"));
        boolean isAssignedUser = projectMilestoneAssignmentRepository.findByProjectIdAndAssignedUserIdAndIsDeletedFalse(projectId,
                userId).isPresent();
        boolean isManagerOfAssignedUser = false;

        if (!isAdmin && !isOperationHead && !isAssignedUser) {
            List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(projectId);
            List<Long> assignedUserIds = assignments.stream()
                    .filter(a -> a.getAssignedUser() != null)
                    .map(a -> a.getAssignedUser().getId())
                    .collect(Collectors.toList());
            List<User> managedUsers = userRepository.findByManagerIdAndIsDeletedFalse(userId);
            isManagerOfAssignedUser = managedUsers.stream().anyMatch(u -> assignedUserIds.contains(u.getId()));
        }

        if (!isAdmin && !isOperationHead && !isAssignedUser && !isManagerOfAssignedUser) {
            logger.warn("User ID {} not authorized to view project ID {}", userId, projectId);
            System.out.println("User ID " + userId + " not authorized to view project ID " + projectId);
            throw new ValidationException("User not authorized to view this project", "ERR_UNAUTHORIZED_ACCESS");
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
                managedUserIds.add(userId);
            }
            assignments = projectMilestoneAssignmentRepository.findByProjectIdAndAssignedUserIdInAndIsVisibleTrueAndStatusIn(
                    projectId, managedUserIds, Arrays.asList(milestoneStatusRepository.findByName("NEW").orElseThrow(),
                            milestoneStatusRepository.findByName("IN_PROGRESS").orElseThrow()));
        } else {
            assignments = projectMilestoneAssignmentRepository.findByProjectIdAndAssignedUserIdAndIsVisibleTrueAndStatusIn(
                    projectId, userId, Arrays.asList(milestoneStatusRepository.findByName("NEW").orElseThrow(),
                            milestoneStatusRepository.findByName("IN_PROGRESS").orElseThrow()));
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
        dto.setCompanyName(project.getCompany() != null ? project.getCompany().getName() : null);

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));
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
                    contactDto.setEmails(contact.getEmails());
                    contactDto.setContactNo(contact.getContactNo());
                    contactDto.setWhatsappNo(contact.getWhatsappNo());
                } else {
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

    @Override
    @Transactional
    public ProjectResponseDto addPaymentByUnbilledNumber(String unbilledNumber, ProjectPaymentTransactionDto dto) {
        validateTransactionDto(dto);
        Project project = projectRepository.findByUnbilledNumberAndIsDeletedFalse(unbilledNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found", "ERR_PROJECT_NOT_FOUND"));
        return addPaymentTransaction(project.getId(), dto);
    }
}