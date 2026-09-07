package com.doc.impl;

import com.doc.dto.productMilestoneMap.ProductMilestoneMapRequestDto;
import com.doc.dto.productMilestoneMap.ProductMilestoneMapResponseDto;
import com.doc.entity.milestone.Milestone;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.repository.MilestoneRepository;
import com.doc.repository.ProductMilestoneMapRepository;
import com.doc.repository.ProductRepository;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.service.ProductMilestoneMapService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing product-milestone mappings.
 *
 * All TAT, reminder and escalation values are stored in minutes.
 */
@Service
@Transactional
public class ProductMilestoneMapServiceImpl
        implements ProductMilestoneMapService {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    ProductMilestoneMapServiceImpl.class
            );

    private static final double MAX_PAYMENT_PERCENTAGE = 100.0;

    private final ProductMilestoneMapRepository
            productMilestoneMapRepository;

    private final ProductRepository productRepository;

    private final MilestoneRepository milestoneRepository;

    private final ProjectMilestoneAssignmentRepository
            projectMilestoneAssignmentRepository;

    public ProductMilestoneMapServiceImpl(
            ProductMilestoneMapRepository
                    productMilestoneMapRepository,
            ProductRepository productRepository,
            MilestoneRepository milestoneRepository,
            ProjectMilestoneAssignmentRepository
                    projectMilestoneAssignmentRepository
    ) {
        this.productMilestoneMapRepository =
                productMilestoneMapRepository;
        this.productRepository = productRepository;
        this.milestoneRepository = milestoneRepository;
        this.projectMilestoneAssignmentRepository =
                projectMilestoneAssignmentRepository;
    }

    // =====================================================================
    // CREATE
    // =====================================================================

    @Override
    public ProductMilestoneMapResponseDto
    createProductMilestoneMap(
            ProductMilestoneMapRequestDto requestDto
    ) {
        logger.info(
                "Creating product-milestone mapping. productId={}, milestoneId={}, order={}",
                requestDto.getProductId(),
                requestDto.getMilestoneId(),
                requestDto.getOrder()
        );

        Product product = findActiveProduct(
                requestDto.getProductId()
        );

        Milestone milestone = findMilestone(
                requestDto.getMilestoneId()
        );

        validateDuplicateOrderForCreate(
                requestDto.getProductId(),
                requestDto.getOrder()
        );

        validateDuplicateMilestoneForCreate(
                requestDto.getProductId(),
                requestDto.getMilestoneId()
        );

        validatePaymentPercentageForCreate(
                requestDto.getProductId(),
                requestDto.getPaymentPercentage()
        );

        ProductMilestoneMap mapping =
                new ProductMilestoneMap();

        applyRequestToEntity(
                mapping,
                product,
                milestone,
                requestDto
        );

        mapping.setDeleted(false);

        ProductMilestoneMap savedMapping =
                productMilestoneMapRepository.save(mapping);

        logger.info(
                "Product-milestone mapping created successfully. mappingId={}, productId={}, milestoneId={}",
                savedMapping.getId(),
                requestDto.getProductId(),
                requestDto.getMilestoneId()
        );

        return mapToResponseDto(savedMapping);
    }

    // =====================================================================
    // UPDATE
    // =====================================================================

    @Override
    public ProductMilestoneMapResponseDto
    updateProductMilestoneMap(
            Long id,
            ProductMilestoneMapRequestDto requestDto
    ) {
        logger.info(
                "Updating product-milestone mapping. mappingId={}, productId={}, milestoneId={}",
                id,
                requestDto.getProductId(),
                requestDto.getMilestoneId()
        );

        ProductMilestoneMap existingMapping =
                findMappingById(id);

        if (existingMapping.isDeleted()) {
            throw new IllegalStateException(
                    "Cannot update deleted product-milestone mapping with ID: "
                            + id
            );
        }

        Product product = findActiveProduct(
                requestDto.getProductId()
        );

        Milestone milestone = findMilestone(
                requestDto.getMilestoneId()
        );

        validateDuplicateOrderForUpdate(
                id,
                requestDto.getProductId(),
                requestDto.getOrder()
        );

        validateDuplicateMilestoneForUpdate(
                id,
                requestDto.getProductId(),
                requestDto.getMilestoneId()
        );

        validatePaymentPercentageForUpdate(
                id,
                requestDto.getProductId(),
                requestDto.getPaymentPercentage()
        );

        applyRequestToEntity(
                existingMapping,
                product,
                milestone,
                requestDto
        );

        ProductMilestoneMap updatedMapping =
                productMilestoneMapRepository.save(
                        existingMapping
                );

        logger.info(
                "Product-milestone mapping updated successfully. mappingId={}",
                updatedMapping.getId()
        );

        return mapToResponseDto(updatedMapping);
    }

    // =====================================================================
    // GET BY ID
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public ProductMilestoneMapResponseDto
    getProductMilestoneMapById(Long id) {
        logger.info(
                "Fetching product-milestone mapping. mappingId={}",
                id
        );

        ProductMilestoneMap mapping =
                findMappingById(id);

        if (mapping.isDeleted()) {
            throw new EntityNotFoundException(
                    "Product-milestone mapping not found with ID: "
                            + id
            );
        }

        return mapToResponseDto(mapping);
    }

    // =====================================================================
    // DELETE
    // =====================================================================

    @Override
    public void deleteProductMilestoneMap(Long id) {
        logger.info(
                "Deleting product-milestone mapping. mappingId={}",
                id
        );

        ProductMilestoneMap mapping =
                findMappingById(id);

        if (mapping.isDeleted()) {
            throw new EntityNotFoundException(
                    "Product-milestone mapping is already deleted with ID: "
                            + id
            );
        }

        boolean alreadyUsed =
                projectMilestoneAssignmentRepository
                        .existsByProductMilestoneMapId(id);

        if (alreadyUsed) {
            logger.warn(
                    "Cannot delete product-milestone mapping because it is already used. mappingId={}",
                    id
            );

            throw new IllegalStateException(
                    "Cannot delete this milestone mapping because "
                            + "it is already used in project milestone assignments."
            );
        }

        /*
         * Hard delete is used because product_id + milestone_id
         * and product_id + step_order have unique constraints.
         *
         * Soft deletion would continue blocking creation of another
         * mapping with the same product, milestone, or order unless
         * the database constraints were redesigned.
         */
        productMilestoneMapRepository.delete(mapping);

        logger.info(
                "Product-milestone mapping deleted successfully. mappingId={}",
                id
        );
    }

    // =====================================================================
    // GET BY USER AND PRODUCT
    // =====================================================================

    @Override
    public List<ProductMilestoneMapResponseDto> getProductMilestoneMapsByUserAndProduct(Long userId, Long productId) {
        logger.info("Fetching product-milestone mappings for user ID: {} and product ID: {}", userId, productId);

        // Fetch mappings by product ID
        List<ProductMilestoneMap> mappings = productMilestoneMapRepository.findByProductId(productId);

        mappings.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));

        return mappings.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // =====================================================================
    // GET BY PRODUCT
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProductMilestoneMapResponseDto>
    getMilestonesByProductId(Long productId) {
        logger.info(
                "Fetching milestones for product. productId={}",
                productId
        );

        findActiveProduct(productId);

        List<ProductMilestoneMapResponseDto> response =
                productMilestoneMapRepository
                        .findByProductId(productId)
                        .stream()
                        .filter(mapping -> !mapping.isDeleted())
                        .sorted(
                                Comparator.comparingInt(
                                        ProductMilestoneMap::getOrder
                                )
                        )
                        .map(this::mapToResponseDto)
                        .collect(Collectors.toList());

        logger.info(
                "Found {} milestone mappings for productId={}",
                response.size(),
                productId
        );

        return response;
    }

    // =====================================================================
    // APPLY REQUEST TO ENTITY
    // =====================================================================

    private void applyRequestToEntity(
            ProductMilestoneMap mapping,
            Product product,
            Milestone milestone,
            ProductMilestoneMapRequestDto requestDto
    ) {
        // Basic mapping
        mapping.setProduct(product);
        mapping.setMilestone(milestone);
        mapping.setOrder(requestDto.getOrder());

        // Execution TAT
        mapping.setExecutionTatApplicable(
                requestDto.isExecutionTatApplicable()
        );
        mapping.setExecutionTatMinutes(
                requestDto.getExecutionTatMinutes()
        );

        // Department TAT
        mapping.setDepartmentTatApplicable(
                requestDto.isDepartmentTatApplicable()
        );
        mapping.setDepartmentTatMinutes(
                requestDto.getDepartmentTatMinutes()
        );

        // Performance TAT
        mapping.setPerformanceTatApplicable(
                requestDto.isPerformanceTatApplicable()
        );
        mapping.setPerformanceTatMinutes(
                requestDto.getPerformanceTatMinutes()
        );

        // Customer / Project SLA TAT
        mapping.setCustomerTatApplicable(
                requestDto.isCustomerTatApplicable()
        );
        mapping.setCustomerTatMinutes(
                requestDto.getCustomerTatMinutes()
        );

        // Rollback / Rework TAT
        mapping.setRollbackTatApplicable(
                requestDto.isRollbackTatApplicable()
        );
        mapping.setRollbackTatMinutes(
                requestDto.getRollbackTatMinutes()
        );

        // Workflow rules
        mapping.setStrictApproval(
                requestDto.isStrictApproval()
        );
        mapping.setAllowRollback(
                requestDto.isAllowRollback()
        );
        mapping.setMaxAttempts(
                requestDto.getMaxAttempts()
        );
        mapping.setMandatory(
                requestDto.isMandatory()
        );
        mapping.setPaymentPercentage(
                requestDto.getPaymentPercentage()
        );
        mapping.setAutoGenerated(
                requestDto.isAutoGenerated()
        );
        mapping.setRequiresPortalDetails(
                requestDto.isRequiresPortalDetails()
        );


        mapping.setBusinessDaysEnabled(
                requestDto.isBusinessDaysEnabled()
        );



        // Common field
        mapping.setActive(
                requestDto.isActive()
        );
    }

    // =====================================================================
    // ENTITY TO RESPONSE DTO
    // =====================================================================

    private ProductMilestoneMapResponseDto mapToResponseDto(
            ProductMilestoneMap mapping
    ) {
        ProductMilestoneMapResponseDto dto =
                new ProductMilestoneMapResponseDto();

        dto.setId(mapping.getId());

        if (mapping.getProduct() != null) {
            dto.setProductId(
                    mapping.getProduct().getId()
            );
            dto.setProductName(
                    mapping.getProduct().getProductName()
            );
        }

        if (mapping.getMilestone() != null) {
            dto.setMilestoneId(
                    mapping.getMilestone().getId()
            );
            dto.setMilestoneName(
                    mapping.getMilestone().getName()
            );
        }

        dto.setOrder(mapping.getOrder());

        // Execution TAT
        dto.setExecutionTatApplicable(
                mapping.isExecutionTatApplicable()
        );
        dto.setExecutionTatMinutes(
                mapping.getExecutionTatMinutes()
        );

        // Department TAT
        dto.setDepartmentTatApplicable(
                mapping.isDepartmentTatApplicable()
        );
        dto.setDepartmentTatMinutes(
                mapping.getDepartmentTatMinutes()
        );

        // Performance TAT
        dto.setPerformanceTatApplicable(
                mapping.isPerformanceTatApplicable()
        );
        dto.setPerformanceTatMinutes(
                mapping.getPerformanceTatMinutes()
        );

        // Customer / Project SLA TAT
        dto.setCustomerTatApplicable(
                mapping.isCustomerTatApplicable()
        );
        dto.setCustomerTatMinutes(
                mapping.getCustomerTatMinutes()
        );

        // Rollback / Rework TAT
        dto.setRollbackTatApplicable(
                mapping.isRollbackTatApplicable()
        );
        dto.setRollbackTatMinutes(
                mapping.getRollbackTatMinutes()
        );

        // Workflow rules
        dto.setStrictApproval(
                mapping.isStrictApproval()
        );
        dto.setAllowRollback(
                mapping.isAllowRollback()
        );
        dto.setMaxAttempts(
                mapping.getMaxAttempts()
        );
        dto.setMandatory(
                mapping.isMandatory()
        );
        dto.setPaymentPercentage(
                mapping.getPaymentPercentage()
        );
        dto.setAutoGenerated(
                mapping.isAutoGenerated()
        );
        dto.setRequiresPortalDetails(
                mapping.isRequiresPortalDetails()
        );

        // TAT behaviour

        dto.setBusinessDaysEnabled(
                mapping.isBusinessDaysEnabled()
        );


        // Common fields
        dto.setActive(mapping.isActive());
        dto.setDeleted(mapping.isDeleted());
        dto.setDate(mapping.getDate());
        dto.setCreatedDate(mapping.getCreatedDate());
        dto.setUpdatedDate(mapping.getUpdatedDate());

        return dto;
    }

    // =====================================================================
    // PRODUCT / MILESTONE VALIDATION
    // =====================================================================

    private Product findActiveProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException(
                    "Product ID must be greater than zero"
            );
        }

        return productRepository
                .findByIdAndIsActiveTrueAndIsDeletedFalse(
                        productId
                )
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Active product not found with ID: "
                                        + productId
                        )
                );
    }

    private Milestone findMilestone(Long milestoneId) {
        if (milestoneId == null || milestoneId <= 0) {
            throw new IllegalArgumentException(
                    "Milestone ID must be greater than zero"
            );
        }

        return milestoneRepository
                .findById(milestoneId)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Milestone not found with ID: "
                                        + milestoneId
                        )
                );
    }

    private ProductMilestoneMap findMappingById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Product-milestone mapping ID must be greater than zero"
            );
        }

        return productMilestoneMapRepository
                .findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Product-milestone mapping not found with ID: "
                                        + id
                        )
                );
    }

    // =====================================================================
    // DUPLICATE VALIDATION - CREATE
    // =====================================================================

    private void validateDuplicateOrderForCreate(
            Long productId,
            int order
    ) {
        boolean duplicateOrder =
                productMilestoneMapRepository
                        .existsByProductIdAndOrder(
                                productId,
                                order
                        );

        if (duplicateOrder) {
            logger.warn(
                    "Duplicate milestone order detected. productId={}, order={}",
                    productId,
                    order
            );

            throw new IllegalArgumentException(
                    "Order " + order
                            + " already exists for product ID: "
                            + productId
            );
        }
    }

    private void validateDuplicateMilestoneForCreate(
            Long productId,
            Long milestoneId
    ) {
        boolean duplicateMilestone =
                productMilestoneMapRepository
                        .findByProductId(productId)
                        .stream()
                        .filter(mapping -> !mapping.isDeleted())
                        .anyMatch(
                                mapping ->
                                        mapping.getMilestone() != null
                                                && milestoneId.equals(
                                                mapping.getMilestone().getId()
                                        )
                        );

        if (duplicateMilestone) {
            logger.warn(
                    "Duplicate product-milestone mapping detected. productId={}, milestoneId={}",
                    productId,
                    milestoneId
            );

            throw new IllegalArgumentException(
                    "Milestone ID " + milestoneId
                            + " already exists for product ID: "
                            + productId
            );
        }
    }

    // =====================================================================
    // DUPLICATE VALIDATION - UPDATE
    // =====================================================================

    private void validateDuplicateOrderForUpdate(
            Long mappingId,
            Long productId,
            int order
    ) {
        boolean duplicateOrder =
                productMilestoneMapRepository
                        .findByProductId(productId)
                        .stream()
                        .filter(mapping -> !mapping.isDeleted())
                        .anyMatch(
                                mapping ->
                                        !mapping.getId().equals(mappingId)
                                                && mapping.getOrder() == order
                        );

        if (duplicateOrder) {
            logger.warn(
                    "Duplicate milestone order detected during update. mappingId={}, productId={}, order={}",
                    mappingId,
                    productId,
                    order
            );

            throw new IllegalArgumentException(
                    "Order " + order
                            + " already exists for product ID: "
                            + productId
            );
        }
    }

    private void validateDuplicateMilestoneForUpdate(
            Long mappingId,
            Long productId,
            Long milestoneId
    ) {
        boolean duplicateMilestone =
                productMilestoneMapRepository
                        .findByProductId(productId)
                        .stream()
                        .filter(mapping -> !mapping.isDeleted())
                        .anyMatch(
                                mapping ->
                                        !mapping.getId().equals(mappingId)
                                                && mapping.getMilestone() != null
                                                && milestoneId.equals(
                                                mapping.getMilestone().getId()
                                        )
                        );

        if (duplicateMilestone) {
            logger.warn(
                    "Duplicate product-milestone mapping detected during update. mappingId={}, productId={}, milestoneId={}",
                    mappingId,
                    productId,
                    milestoneId
            );

            throw new IllegalArgumentException(
                    "Milestone ID " + milestoneId
                            + " already exists for product ID: "
                            + productId
            );
        }
    }

    // =====================================================================
    // PAYMENT PERCENTAGE VALIDATION
    // =====================================================================

    private void validatePaymentPercentageForCreate(
            Long productId,
            double requestedPercentage
    ) {
        double currentTotal =
                productMilestoneMapRepository
                        .findByProductId(productId)
                        .stream()
                        .filter(mapping -> !mapping.isDeleted())
                        .mapToDouble(
                                ProductMilestoneMap::getPaymentPercentage
                        )
                        .sum();

        double newTotal =
                currentTotal + requestedPercentage;

        if (newTotal > MAX_PAYMENT_PERCENTAGE) {
            logger.warn(
                    "Payment percentage exceeded during create. productId={}, currentTotal={}, requestedPercentage={}, newTotal={}",
                    productId,
                    currentTotal,
                    requestedPercentage,
                    newTotal
            );

            throw new IllegalArgumentException(
                    String.format(
                            "Cannot create milestone: total payment percentage "
                                    + "would be %.2f%%. Maximum allowed is 100%%. "
                                    + "Current total is %.2f%%.",
                            newTotal,
                            currentTotal
                    )
            );
        }
    }

    private void validatePaymentPercentageForUpdate(
            Long mappingId,
            Long productId,
            double requestedPercentage
    ) {
        double currentTotalExcludingMapping =
                productMilestoneMapRepository
                        .findByProductId(productId)
                        .stream()
                        .filter(mapping -> !mapping.isDeleted())
                        .filter(
                                mapping ->
                                        !mapping.getId().equals(mappingId)
                        )
                        .mapToDouble(
                                ProductMilestoneMap::getPaymentPercentage
                        )
                        .sum();

        double newTotal =
                currentTotalExcludingMapping
                        + requestedPercentage;

        if (newTotal > MAX_PAYMENT_PERCENTAGE) {
            logger.warn(
                    "Payment percentage exceeded during update. mappingId={}, productId={}, currentTotalExcludingMapping={}, requestedPercentage={}, newTotal={}",
                    mappingId,
                    productId,
                    currentTotalExcludingMapping,
                    requestedPercentage,
                    newTotal
            );

            throw new IllegalArgumentException(
                    String.format(
                            "Cannot update milestone: total payment percentage "
                                    + "would be %.2f%%. Maximum allowed is 100%%. "
                                    + "Current total excluding this milestone is %.2f%%.",
                            newTotal,
                            currentTotalExcludingMapping
                    )
            );
        }
    }
}