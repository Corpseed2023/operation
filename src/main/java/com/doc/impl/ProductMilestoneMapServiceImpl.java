package com.doc.impl;

import com.doc.dto.productMilestoneMap.ProductMilestoneMapRequestDto;
import com.doc.dto.productMilestoneMap.ProductMilestoneMapResponseDto;
import com.doc.entity.milestone.Milestone;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.repository.*;
import com.doc.service.ProductMilestoneMapService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing ProductMilestoneMap entities.
 */
@Service
@Transactional
public class ProductMilestoneMapServiceImpl implements ProductMilestoneMapService {

    private static final Logger logger =
            LoggerFactory.getLogger(ProductMilestoneMapServiceImpl.class);

    private final ProductMilestoneMapRepository productMilestoneMapRepository;
    private final ProductRepository productRepository;
    private final ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserProductMapRepository userProductMapRepository;

    @Autowired
    public ProductMilestoneMapServiceImpl(
            ProductMilestoneMapRepository productMilestoneMapRepository,
            ProductRepository productRepository,
            ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository,
            MilestoneRepository milestoneRepository,
            UserProductMapRepository userProductMapRepository
    ) {
        this.productMilestoneMapRepository = productMilestoneMapRepository;
        this.productRepository = productRepository;
        this.projectMilestoneAssignmentRepository = projectMilestoneAssignmentRepository;
        this.milestoneRepository = milestoneRepository;
        this.userProductMapRepository = userProductMapRepository;
    }

    @Override
    public ProductMilestoneMapResponseDto createProductMilestoneMap(ProductMilestoneMapRequestDto requestDto) {
        logger.info("Creating product-milestone mapping for product ID: {} and milestone ID: {}",
                requestDto.getProductId(), requestDto.getMilestoneId());

        Product product = productRepository.findActiveUserById(requestDto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product not found with ID: " + requestDto.getProductId()
                ));

        Milestone milestone = milestoneRepository.findById(requestDto.getMilestoneId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Milestone not found with ID: " + requestDto.getMilestoneId()
                ));

        /*
         * Check duplicate step order for same product.
         */
        if (productMilestoneMapRepository.existsByProductIdAndOrder(
                requestDto.getProductId(),
                requestDto.getOrder()
        )) {
            throw new IllegalArgumentException(
                    "Order " + requestDto.getOrder()
                            + " already exists for product ID: " + requestDto.getProductId()
            );
        }

        /*
         * Check duplicate product + milestone.
         * This protects uk_product_milestone constraint.
         */
        boolean duplicateProductMilestoneExists = productMilestoneMapRepository
                .findByProductId(requestDto.getProductId())
                .stream()
                .anyMatch(mapping ->
                        mapping.getMilestone() != null
                                && mapping.getMilestone().getId().equals(requestDto.getMilestoneId())
                );

        if (duplicateProductMilestoneExists) {
            throw new IllegalArgumentException(
                    "Milestone ID " + requestDto.getMilestoneId()
                            + " already exists for product ID: " + requestDto.getProductId()
            );
        }

        /*
         * Validate total payment percentage.
         */
        double currentSum = productMilestoneMapRepository
                .findByProductId(requestDto.getProductId())
                .stream()
                .mapToDouble(ProductMilestoneMap::getPaymentPercentage)
                .sum();

        double newTotal = currentSum + requestDto.getPaymentPercentage();

        if (newTotal > 100.0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Cannot create milestone: total payment percentage would be %.1f%% (maximum allowed is 100%%). Current sum for product %d is %.1f%%.",
                            newTotal,
                            requestDto.getProductId(),
                            currentSum
                    )
            );
        }

        ProductMilestoneMap mapping = new ProductMilestoneMap();

        /*
         * Basic mapping
         */
        mapping.setProduct(product);
        mapping.setMilestone(milestone);
        mapping.setOrder(requestDto.getOrder());
        mapping.setTatInDays(requestDto.getTatInDays());

        /*
         * Execution TAT - user-wise
         */
        mapping.setExecutionTatApplicable(requestDto.isExecutionTatApplicable());
        mapping.setExecutionTatInDays(requestDto.getExecutionTatInDays());
        mapping.setExecutionTatHours(requestDto.getExecutionTatHours());

        /*
         * Department TAT
         */
        mapping.setDepartmentTatApplicable(requestDto.isDepartmentTatApplicable());
        mapping.setDepartmentTatHours(requestDto.getDepartmentTatHours());

        /*
         * Performance TAT
         */
        mapping.setPerformanceTatApplicable(requestDto.isPerformanceTatApplicable());
        mapping.setPerformanceTatHours(requestDto.getPerformanceTatHours());

        /*
         * Customer / project SLA TAT
         */
        mapping.setCustomerTatApplicable(requestDto.isCustomerTatApplicable());
        mapping.setCustomerTatHours(requestDto.getCustomerTatHours());

        /*
         * Rollback TAT
         */
        mapping.setRollbackTatApplicable(requestDto.isRollbackTatApplicable());
        mapping.setRollbackTatInDays(requestDto.getRollbackTatInDays());
        mapping.setRollbackTatHours(requestDto.getRollbackTatHours());

        /*
         * Workflow rules
         */
        mapping.setStrictApproval(requestDto.isStrictApproval());
        mapping.setAllowRollback(requestDto.isAllowRollback());
        mapping.setMaxAttempts(requestDto.getMaxAttempts());
        mapping.setMandatory(requestDto.isMandatory());
        mapping.setPaymentPercentage(requestDto.getPaymentPercentage());
        mapping.setAutoGenerated(requestDto.isAutoGenerated());
        mapping.setRequiresPortalDetails(requestDto.isRequiresPortalDetails());

        /*
         * TAT behaviour
         */
        mapping.setAllowTatResetOnReassign(requestDto.isAllowTatResetOnReassign());
        mapping.setBusinessDaysEnabled(requestDto.isBusinessDaysEnabled());

        /*
         * Reminder / escalation
         */
        mapping.setReminderBeforeDueHours(requestDto.getReminderBeforeDueHours());
        mapping.setManagerEscalationAfterDueHours(requestDto.getManagerEscalationAfterDueHours());
        mapping.setHodEscalationAfterDueHours(requestDto.getHodEscalationAfterDueHours());

        /*
         * Common fields
         */
        mapping.setActive(requestDto.isActive());
        mapping.setDeleted(false);
        mapping.setCreatedDate(new Date());
        mapping.setUpdatedDate(new Date());
        mapping.setDate(LocalDate.now());

        ProductMilestoneMap savedMapping = productMilestoneMapRepository.save(mapping);

        logger.info("Product-milestone mapping created with ID: {}", savedMapping.getId());

        return mapToResponseDto(savedMapping);
    }



    @Override
    public ProductMilestoneMapResponseDto updateProductMilestoneMap(Long id, ProductMilestoneMapRequestDto requestDto) {
        logger.info("Updating product-milestone mapping with ID: {}", id);

        ProductMilestoneMap existingMapping = productMilestoneMapRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product-milestone mapping not found with ID: " + id
                ));

        Product product = productRepository.findActiveUserById(requestDto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product not found with ID: " + requestDto.getProductId()
                ));

        Milestone milestone = milestoneRepository.findById(requestDto.getMilestoneId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Milestone not found with ID: " + requestDto.getMilestoneId()
                ));

        /*
         * Check duplicate step order for same product excluding current mapping.
         */
        boolean duplicateOrderExists = productMilestoneMapRepository
                .findByProductId(requestDto.getProductId())
                .stream()
                .anyMatch(mapping ->
                        !mapping.getId().equals(id)
                                && mapping.getOrder() == requestDto.getOrder()
                );

        if (duplicateOrderExists) {
            throw new IllegalArgumentException(
                    "Order " + requestDto.getOrder()
                            + " already exists for product ID: " + requestDto.getProductId()
            );
        }

        /*
         * Check duplicate product + milestone excluding current mapping.
         * This protects your uk_product_milestone constraint.
         */
        boolean duplicateProductMilestoneExists = productMilestoneMapRepository
                .findByProductId(requestDto.getProductId())
                .stream()
                .anyMatch(mapping ->
                        !mapping.getId().equals(id)
                                && mapping.getMilestone() != null
                                && mapping.getMilestone().getId().equals(requestDto.getMilestoneId())
                );

        if (duplicateProductMilestoneExists) {
            throw new IllegalArgumentException(
                    "Milestone ID " + requestDto.getMilestoneId()
                            + " already exists for product ID: " + requestDto.getProductId()
            );
        }

        /*
         * Validate total payment percentage for selected product excluding current mapping.
         * Important: use requestDto.getProductId(), not existingMapping.getProduct().getId(),
         * because product may also be changed during update.
         */
        double currentSumExcludingThis = productMilestoneMapRepository
                .findByProductId(requestDto.getProductId())
                .stream()
                .filter(mapping -> !mapping.getId().equals(id))
                .mapToDouble(ProductMilestoneMap::getPaymentPercentage)
                .sum();

        double newTotal = currentSumExcludingThis + requestDto.getPaymentPercentage();

        if (newTotal > 100.0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Update would make total payment percentage %.1f%% (max 100%%). Current sum excluding this milestone: %.1f%%",
                            newTotal,
                            currentSumExcludingThis
                    )
            );
        }

        /*
         * Basic mapping
         */
        existingMapping.setProduct(product);
        existingMapping.setMilestone(milestone);
        existingMapping.setOrder(requestDto.getOrder());
        existingMapping.setTatInDays(requestDto.getTatInDays());

        /*
         * Execution TAT - user-wise
         */
        existingMapping.setExecutionTatApplicable(requestDto.isExecutionTatApplicable());
        existingMapping.setExecutionTatInDays(requestDto.getExecutionTatInDays());
        existingMapping.setExecutionTatHours(requestDto.getExecutionTatHours());

        /*
         * Department TAT
         */
        existingMapping.setDepartmentTatApplicable(requestDto.isDepartmentTatApplicable());
        existingMapping.setDepartmentTatHours(requestDto.getDepartmentTatHours());

        /*
         * Performance TAT
         */
        existingMapping.setPerformanceTatApplicable(requestDto.isPerformanceTatApplicable());
        existingMapping.setPerformanceTatHours(requestDto.getPerformanceTatHours());

        /*
         * Customer / project SLA TAT
         */
        existingMapping.setCustomerTatApplicable(requestDto.isCustomerTatApplicable());
        existingMapping.setCustomerTatHours(requestDto.getCustomerTatHours());

        /*
         * Rollback TAT
         */
        existingMapping.setRollbackTatApplicable(requestDto.isRollbackTatApplicable());
        existingMapping.setRollbackTatInDays(requestDto.getRollbackTatInDays());
        existingMapping.setRollbackTatHours(requestDto.getRollbackTatHours());

        /*
         * Workflow rules
         */
        existingMapping.setStrictApproval(requestDto.isStrictApproval());
        existingMapping.setAllowRollback(requestDto.isAllowRollback());
        existingMapping.setMaxAttempts(requestDto.getMaxAttempts());
        existingMapping.setMandatory(requestDto.isMandatory());
        existingMapping.setPaymentPercentage(requestDto.getPaymentPercentage());
        existingMapping.setAutoGenerated(requestDto.isAutoGenerated());
        existingMapping.setRequiresPortalDetails(requestDto.isRequiresPortalDetails());

        /*
         * TAT behaviour
         */
        existingMapping.setAllowTatResetOnReassign(requestDto.isAllowTatResetOnReassign());
        existingMapping.setBusinessDaysEnabled(requestDto.isBusinessDaysEnabled());

        /*
         * Reminder / escalation
         */
        existingMapping.setReminderBeforeDueHours(requestDto.getReminderBeforeDueHours());
        existingMapping.setManagerEscalationAfterDueHours(requestDto.getManagerEscalationAfterDueHours());
        existingMapping.setHodEscalationAfterDueHours(requestDto.getHodEscalationAfterDueHours());

        /*
         * Common fields
         */
        existingMapping.setActive(requestDto.isActive());
        existingMapping.setUpdatedDate(new Date());

        ProductMilestoneMap updatedMapping = productMilestoneMapRepository.save(existingMapping);

        logger.info("Product-milestone mapping updated with ID: {}", updatedMapping.getId());

        return mapToResponseDto(updatedMapping);
    }
    @Override
    public ProductMilestoneMapResponseDto getProductMilestoneMapById(Long id) {
        logger.info("Fetching product-milestone mapping with ID: {}", id);
        ProductMilestoneMap mapping = productMilestoneMapRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product-milestone mapping not found with ID: " + id));
        return mapToResponseDto(mapping);
    }

    @Override
    public void deleteProductMilestoneMap(Long id) {
        logger.info("Deleting product-milestone mapping with ID: {}", id);

        ProductMilestoneMap mapping = productMilestoneMapRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product-milestone mapping not found with ID: " + id
                ));

        boolean alreadyUsed =
                projectMilestoneAssignmentRepository.existsByProductMilestoneMapId(id);

        if (alreadyUsed) {
            throw new IllegalStateException(
                    "Cannot delete this milestone mapping because it is already used in project milestone assignments."
            );
        }

        productMilestoneMapRepository.delete(mapping);

        logger.info("Product-milestone mapping deleted with ID: {}", id);
    }

    @Override
    public List<ProductMilestoneMapResponseDto> getProductMilestoneMapsByUserAndProduct(Long userId, Long productId) {
        logger.info("Fetching product-milestone mappings for user ID: {} and product ID: {}", userId, productId);

        // Fetch mappings by product ID
        List<ProductMilestoneMap> mappings = productMilestoneMapRepository.findByProductId(productId);
        return mappings.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductMilestoneMapResponseDto> getMilestonesByProductId(Long productId) {
        logger.info("Fetching milestones with payment percentages for product ID: {}", productId);

        // Optional: Validate product exists and is active
        productRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(productId)
                .orElseThrow(() -> new EntityNotFoundException("Active product not found with ID: " + productId));

        List<ProductMilestoneMap> mappings = productMilestoneMapRepository.findByProductId(productId);

        // Sort by step order (important for workflow)
        mappings.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));

        return mappings.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }


    private ProductMilestoneMapResponseDto mapToResponseDto(ProductMilestoneMap mapping) {
        ProductMilestoneMapResponseDto dto = new ProductMilestoneMapResponseDto();

        dto.setId(mapping.getId());

        if (mapping.getProduct() != null) {
            dto.setProductId(mapping.getProduct().getId());
            dto.setProductName(mapping.getProduct().getProductName());
        }

        if (mapping.getMilestone() != null) {
            dto.setMilestoneId(mapping.getMilestone().getId());
            dto.setMilestoneName(mapping.getMilestone().getName());
        }

        dto.setOrder(mapping.getOrder());

        dto.setTatInDays(mapping.getTatInDays());

        // =====================================================================
        // EXECUTION TAT
        // =====================================================================

        dto.setExecutionTatApplicable(mapping.isExecutionTatApplicable());
        dto.setExecutionTatInDays(mapping.getExecutionTatInDays());
        dto.setExecutionTatHours(mapping.getExecutionTatHours());

        // =====================================================================
        // DEPARTMENT TAT
        // =====================================================================

        dto.setDepartmentTatApplicable(mapping.isDepartmentTatApplicable());
        dto.setDepartmentTatHours(mapping.getDepartmentTatHours());

        // =====================================================================
        // PERFORMANCE TAT
        // =====================================================================

        dto.setPerformanceTatApplicable(mapping.isPerformanceTatApplicable());
        dto.setPerformanceTatHours(mapping.getPerformanceTatHours());

        // =====================================================================
        // CUSTOMER / PROJECT SLA TAT
        // =====================================================================

        dto.setCustomerTatApplicable(mapping.isCustomerTatApplicable());
        dto.setCustomerTatHours(mapping.getCustomerTatHours());

        // =====================================================================
        // ROLLBACK TAT
        // =====================================================================

        dto.setRollbackTatApplicable(mapping.isRollbackTatApplicable());
        dto.setRollbackTatInDays(mapping.getRollbackTatInDays());
        dto.setRollbackTatHours(mapping.getRollbackTatHours());

        // =====================================================================
        // WORKFLOW RULES
        // =====================================================================

        dto.setStrictApproval(mapping.isStrictApproval());
        dto.setAllowRollback(mapping.isAllowRollback());
        dto.setMaxAttempts(mapping.getMaxAttempts());
        dto.setMandatory(mapping.isMandatory());
        dto.setPaymentPercentage(mapping.getPaymentPercentage());
        dto.setAutoGenerated(mapping.isAutoGenerated());
        dto.setRequiresPortalDetails(mapping.isRequiresPortalDetails());

        dto.setAllowTatResetOnReassign(mapping.isAllowTatResetOnReassign());
        dto.setBusinessDaysEnabled(mapping.isBusinessDaysEnabled());

        // =====================================================================
        // REMINDER / ESCALATION
        // =====================================================================

        dto.setReminderBeforeDueHours(mapping.getReminderBeforeDueHours());
        dto.setManagerEscalationAfterDueHours(mapping.getManagerEscalationAfterDueHours());
        dto.setHodEscalationAfterDueHours(mapping.getHodEscalationAfterDueHours());

        // =====================================================================
        // COMMON FLAGS
        // =====================================================================

        dto.setActive(mapping.isActive());
        dto.setDeleted(mapping.isDeleted());

        return dto;
    }
}