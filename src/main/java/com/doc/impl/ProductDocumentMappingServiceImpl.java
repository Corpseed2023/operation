package com.doc.impl;

import com.doc.dto.document.ProductDocumentMappingRequestDto;
import com.doc.dto.document.ProductDocumentMappingResponseDto;
import com.doc.entity.document.ApplicantType;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.product.Product;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProductDocumentMappingRepository;
import com.doc.repository.ProductRepository;
import com.doc.repository.documentRepo.ApplicantTypeRepository;
import com.doc.repository.documentRepo.ProductRequiredDocumentsRepository;
import com.doc.service.ProductDocumentMappingService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDocumentMappingServiceImpl
        implements ProductDocumentMappingService {

    private static final Logger logger =
            LogManager.getLogger(ProductDocumentMappingServiceImpl.class);

    private final ProductDocumentMappingRepository mappingRepository;
    private final ProductRequiredDocumentsRepository requiredDocumentsRepository;
    private final ApplicantTypeRepository applicantTypeRepository;
    private final ProductRepository productRepository;

    // =====================================================================
    // ASSIGN DOCUMENTS
    // =====================================================================

    @Override
    @Transactional
    public void assignDocuments(ProductDocumentMappingRequestDto request) {

        logger.info(
                "Assign document request received. "
                        + "productId={}, applicantTypeIds={}, requiredDocumentIds={}, updatedBy={}",
                request != null ? request.getProductId() : null,
                request != null ? request.getApplicantTypeIds() : null,
                request != null ? request.getRequiredDocumentIds() : null,
                request != null ? request.getUpdatedBy() : null
        );

        validateAssignRequest(request);

        Product product = productRepository
                .findByIdAndIsActiveTrueAndIsDeletedFalse(
                        request.getProductId()
                )
                .orElseThrow(() -> {

                    logger.warn(
                            "Product not found/inactive/deleted. productId={}",
                            request.getProductId()
                    );

                    return new ResourceNotFoundException(
                            "The selected product was not found or is inactive. "
                                    + "Please select an active product.",
                            "ERR_PRODUCT_NOT_FOUND"
                    );
                });

        Long updatedBy = request.getUpdatedBy();

        /*
         * Nothing to map.
         *
         * Existing mappings are NOT removed.
         */
        if (request.getRequiredDocumentIds() == null
                || request.getRequiredDocumentIds().isEmpty()) {

            logger.info(
                    "No required documents supplied. "
                            + "No mapping changes required. productId={}",
                    product.getId()
            );

            return;
        }

        /*
         * Remove duplicates from document IDs while preserving order.
         */
        List<Long> requestedDocumentIds =
                new ArrayList<>(
                        new LinkedHashSet<>(
                                request.getRequiredDocumentIds()
                        )
                );

        /*
         * Fetch documents.
         */
        List<ProductRequiredDocuments> requiredDocuments =
                requiredDocumentsRepository
                        .findAllByIdInAndIsActiveTrueAndIsDeletedFalse(
                                requestedDocumentIds
                        );

        Map<Long, ProductRequiredDocuments> documentById =
                requiredDocuments.stream()
                        .collect(
                                Collectors.toMap(
                                        ProductRequiredDocuments::getId,
                                        document -> document
                                )
                        );

        /*
         * Validate documents.
         */
        Set<Long> missingOrInactiveDocumentIds =
                requestedDocumentIds.stream()
                        .filter(documentId ->
                                !documentById.containsKey(documentId)
                        )
                        .collect(
                                Collectors.toCollection(
                                        LinkedHashSet::new
                                )
                        );

        if (!missingOrInactiveDocumentIds.isEmpty()) {

            logger.warn(
                    "Required documents not found/inactive. "
                            + "productId={}, invalidDocumentIds={}",
                    product.getId(),
                    missingOrInactiveDocumentIds
            );

            throw new ResourceNotFoundException(
                    "The following required document IDs were not found or are inactive: "
                            + missingOrInactiveDocumentIds
                            + ". Please select valid active documents.",
                    "ERR_REQUIRED_DOCUMENTS_NOT_FOUND"
            );
        }

        /*
         * Multiple applicant types.
         *
         * Duplicate applicant IDs in the request are removed.
         */
        List<Long> requestedApplicantTypeIds =
                request.getApplicantTypeIds() == null
                        ? List.of()
                        : new ArrayList<>(
                        new LinkedHashSet<>(
                                request.getApplicantTypeIds()
                        )
                );

        /*
         * If no applicant type is provided, preserve the existing
         * global-document functionality.
         */
        List<ApplicantType> applicantTypes =
                new ArrayList<>();

        if (!requestedApplicantTypeIds.isEmpty()) {

            for (Long applicantTypeId : requestedApplicantTypeIds) {

                ApplicantType applicantType =
                        resolveApplicantType(
                                applicantTypeId
                        );

                applicantTypes.add(applicantType);
            }

        } else {

            /*
             * null represents global mapping.
             */
            applicantTypes.add(null);
        }

        List<ProductDocumentMapping> mappingsToSave =
                new ArrayList<>();

        int newMappingCount = 0;
        int existingMappingCount = 0;

        /*
         * Applicant Type × Required Documents
         *
         * Example:
         *
         * applicants = [4, 5]
         * documents  = [1, 2]
         *
         * creates:
         *
         * 4 + 1
         * 4 + 2
         * 5 + 1
         * 5 + 2
         */
        for (ApplicantType applicantType : applicantTypes) {

            Long applicantTypeId =
                    applicantType != null
                            ? applicantType.getId()
                            : null;

            for (int documentIndex = 0;
                 documentIndex < requestedDocumentIds.size();
                 documentIndex++) {

                Long documentId =
                        requestedDocumentIds.get(documentIndex);

                ProductRequiredDocuments document =
                        documentById.get(documentId);

                Optional<ProductDocumentMapping> existingMappingOptional =
                        findExactMapping(
                                product.getId(),
                                documentId,
                                applicantTypeId
                        );

                /*
                 * Exact mapping already exists.
                 *
                 * Reuse it instead of inserting duplicate row.
                 */
                if (existingMappingOptional.isPresent()) {

                    ProductDocumentMapping existingMapping =
                            existingMappingOptional.get();

                    existingMapping.setMandatory(true);
                    existingMapping.setDisplayOrder(
                            documentIndex + 1
                    );
                    existingMapping.setActive(true);
                    existingMapping.setUpdatedBy(updatedBy);

                    mappingsToSave.add(
                            existingMapping
                    );

                    existingMappingCount++;

                    logger.debug(
                            "Existing mapping reused. "
                                    + "mappingId={}, productId={}, applicantTypeId={}, "
                                    + "documentId={}",
                            existingMapping.getId(),
                            product.getId(),
                            applicantTypeId,
                            documentId
                    );

                    continue;
                }

                /*
                 * New mapping.
                 */
                ProductDocumentMapping mapping =
                        new ProductDocumentMapping();

                mapping.setProduct(product);

                mapping.setRequiredDocument(
                        document
                );

                mapping.setApplicantType(
                        applicantType
                );

                mapping.setMandatory(true);

                mapping.setDisplayOrder(
                        documentIndex + 1
                );

                mapping.setActive(true);

                mapping.setCreatedBy(
                        updatedBy
                );

                mapping.setUpdatedBy(
                        updatedBy
                );

                mappingsToSave.add(
                        mapping
                );

                newMappingCount++;

                logger.debug(
                        "New mapping prepared. "
                                + "productId={}, applicantTypeId={}, documentId={}, displayOrder={}",
                        product.getId(),
                        applicantTypeId,
                        documentId,
                        documentIndex + 1
                );
            }
        }

        try {

            if (!mappingsToSave.isEmpty()) {

                mappingRepository.saveAllAndFlush(
                        mappingsToSave
                );
            }

        } catch (DataIntegrityViolationException ex) {

            logger.error(
                    "Document mapping database conflict. "
                            + "productId={}, applicantTypeIds={}, documentIds={}",
                    product.getId(),
                    requestedApplicantTypeIds,
                    requestedDocumentIds,
                    ex
            );

            throw new ValidationException(
                    "One or more selected documents are already mapped to one "
                            + "of the selected applicant types. "
                            + "Please refresh and try again.",
                    "ERR_DOCUMENT_MAPPING_CONFLICT"
            );
        }

        logger.info(
                "Document assignment completed successfully. "
                        + "productId={}, applicantTypeIds={}, documentIds={}, "
                        + "newMappings={}, existingMappingsReused={}",
                product.getId(),
                requestedApplicantTypeIds,
                requestedDocumentIds,
                newMappingCount,
                existingMappingCount
        );
    }

    @Override
    @Transactional
    public void deleteDocumentMapping(Long productId, Long mappingId, Long updatedBy) {

        logger.info("Deleting document mapping. productId={}, mappingId={}, updatedBy={}",
                productId, mappingId, updatedBy);

        if (productId == null || productId <= 0) {
            throw new ValidationException(
                    "A valid Product ID is required.", "ERR_INVALID_PRODUCT_ID");
        }
        if (mappingId == null || mappingId <= 0) {
            throw new ValidationException(
                    "A valid mapping ID is required.", "ERR_INVALID_MAPPING_ID");
        }
        if (updatedBy == null || updatedBy <= 0) {
            throw new ValidationException(
                    "Updated By user ID is required.", "ERR_MISSING_UPDATED_BY");
        }

        ProductDocumentMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document mapping with ID " + mappingId + " was not found.",
                        "ERR_MAPPING_NOT_FOUND"));

        if (mapping.getProduct() == null
                || !productId.equals(mapping.getProduct().getId())) {

            logger.warn("Mapping/product mismatch on delete. mappingId={}, productId={}",
                    mappingId, productId);

            throw new ValidationException(
                    "Mapping " + mappingId + " does not belong to product " + productId + ".",
                    "ERR_MAPPING_PRODUCT_MISMATCH");
        }

        if (!mapping.isActive()) {
            throw new ResourceNotFoundException(
                    "Document mapping with ID " + mappingId + " is already removed.",
                    "ERR_MAPPING_ALREADY_REMOVED");
        }

        mapping.setActive(false);
        mapping.setUpdatedBy(updatedBy);
        mappingRepository.saveAndFlush(mapping);

        logger.info("Document mapping deactivated. mappingId={}, productId={}",
                mappingId, productId);
    }



    // =====================================================================
    // GET REQUIRED DOCUMENTS
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProductDocumentMappingResponseDto> getRequiredDocuments(
            Long productId,
            Long applicantTypeId
    ) {

        logger.debug(
                "Fetching required documents. productId={}, applicantTypeId={}",
                productId,
                applicantTypeId
        );

        validateProductExists(productId);

        List<ProductDocumentMapping> mappings;

        /*
         * Existing behaviour retained:
         *
         * if applicantTypeId is not supplied,
         * return all active mappings for the product.
         */
        if (applicantTypeId == null
                || applicantTypeId == -1) {

            mappings =
                    mappingRepository
                            .findByProductIdAndIsActiveTrue(
                                    productId
                            );

        } else {

            /*
             * Validate applicant type so frontend receives proper 404
             * instead of silently receiving empty data for an invalid ID.
             */
            resolveApplicantType(applicantTypeId);

            mappings =
                    mappingRepository
                            .findByProductIdAndApplicantTypeIdAndIsActiveTrue(
                                    productId,
                                    applicantTypeId
                            );
        }

        List<ProductDocumentMappingResponseDto> response =
                mappings.stream()
                        .sorted(
                                Comparator.comparingInt(
                                        mapping ->
                                                mapping.getDisplayOrder() != null
                                                        ? mapping.getDisplayOrder()
                                                        : Integer.MAX_VALUE
                                )
                        )
                        .map(this::mapToResponseDtoInDocs)
                        .toList();

        logger.debug(
                "Required documents fetched successfully. "
                        + "productId={}, applicantTypeId={}, count={}",
                productId,
                applicantTypeId,
                response.size()
        );

        return response;
    }

    // =====================================================================
    // FIND EXACT MAPPING
    // =====================================================================

    private Optional<ProductDocumentMapping> findExactMapping(
            Long productId,
            Long documentId,
            Long applicantTypeId
    ) {

        if (applicantTypeId == null) {

            return mappingRepository
                    .findExactGlobalMapping(
                            productId,
                            documentId
                    );
        }

        return mappingRepository
                .findExactMapping(
                        productId,
                        documentId,
                        applicantTypeId
                );
    }
    // =====================================================================
    // RESPONSE MAPPING
    // =====================================================================

    private ProductDocumentMappingResponseDto mapToResponseDtoInDocs(
            ProductDocumentMapping mapping
    ) {

        ProductRequiredDocuments document =
                mapping.getRequiredDocument();

        ApplicantType applicantType =
                mapping.getApplicantType();

        return new ProductDocumentMappingResponseDto(
                mapping.getId(),
                document.getId(),
                document.getName(),
                document.getType(),
                document.getDescription(),
                mapping.isMandatory(),
                mapping.getDisplayOrder(),
                document.getAllowedFormats(),
                document.getExpiryType(),
                document.getExpiryTypeDescription(),
                document.getMaxValidityYears(),
                applicantType != null
                        ? applicantType.getId()
                        : null,
                applicantType != null
                        ? applicantType.getName()
                        : null
        );
    }

    // =====================================================================
    // VALIDATION
    // =====================================================================

    private void validateAssignRequest(
            ProductDocumentMappingRequestDto request
    ) {

        if (request == null) {

            logger.warn(
                    "Document mapping rejected because request body is null"
            );

            throw new ValidationException(
                    "Document mapping request body is required.",
                    "ERR_DOCUMENT_MAPPING_REQUEST_REQUIRED"
            );
        }

        if (request.getProductId() == null) {

            throw new ValidationException(
                    "Product ID is required to assign documents.",
                    "ERR_MISSING_PRODUCT_ID"
            );
        }

        if (request.getProductId() <= 0) {

            throw new ValidationException(
                    "Product ID must be greater than zero.",
                    "ERR_INVALID_PRODUCT_ID"
            );
        }

        if (request.getUpdatedBy() == null) {

            throw new ValidationException(
                    "Updated By user ID is required to assign documents.",
                    "ERR_MISSING_UPDATED_BY"
            );
        }

        if (request.getUpdatedBy() <= 0) {

            throw new ValidationException(
                    "Updated By user ID must be greater than zero.",
                    "ERR_INVALID_UPDATED_BY"
            );
        }

        /*
         * Validate multiple applicant IDs.
         */
        if (request.getApplicantTypeIds() != null) {

            if (request.getApplicantTypeIds().contains(null)) {

                throw new ValidationException(
                        "Applicant Type IDs cannot contain null values.",
                        "ERR_INVALID_APPLICANT_TYPE_IDS"
                );
            }

            boolean invalidApplicantTypeId =
                    request.getApplicantTypeIds()
                            .stream()
                            .anyMatch(id -> id <= 0);

            if (invalidApplicantTypeId) {

                throw new ValidationException(
                        "Every Applicant Type ID must be greater than zero.",
                        "ERR_INVALID_APPLICANT_TYPE_IDS"
                );
            }
        }

        /*
         * Validate multiple document IDs.
         */
        if (request.getRequiredDocumentIds() != null) {

            if (request.getRequiredDocumentIds().contains(null)) {

                throw new ValidationException(
                        "Required document IDs cannot contain null values.",
                        "ERR_INVALID_DOCUMENT_IDS"
                );
            }

            boolean invalidDocumentId =
                    request.getRequiredDocumentIds()
                            .stream()
                            .anyMatch(id -> id <= 0);

            if (invalidDocumentId) {

                throw new ValidationException(
                        "Every required document ID must be greater than zero.",
                        "ERR_INVALID_DOCUMENT_IDS"
                );
            }
        }
    }

    // =====================================================================
    // PRODUCT VALIDATION
    // =====================================================================

    private void validateProductExists(Long productId) {

        if (productId == null || productId <= 0) {

            throw new ValidationException(
                    "A valid Product ID is required.",
                    "ERR_INVALID_PRODUCT_ID"
            );
        }

        productRepository
                .findByIdAndIsActiveTrueAndIsDeletedFalse(
                        productId
                )
                .orElseThrow(() -> {

                    logger.warn(
                            "Product validation failed. productId={}",
                            productId
                    );

                    return new ResourceNotFoundException(
                            "Product with ID "
                                    + productId
                                    + " was not found or is inactive.",
                            "ERR_PRODUCT_NOT_FOUND"
                    );
                });
    }

    // =====================================================================
    // APPLICANT TYPE VALIDATION
    // =====================================================================

    private ApplicantType resolveApplicantType(
            Long applicantTypeId
    ) {

        /*
         * NULL means global document.
         */
        if (applicantTypeId == null) {
            return null;
        }

        return applicantTypeRepository
                .findByIdAndIsActiveTrueAndIsDeletedFalse(
                        applicantTypeId
                )
                .orElseThrow(() -> {

                    logger.warn(
                            "Applicant Type not found/inactive. applicantTypeId={}",
                            applicantTypeId
                    );

                    return new ResourceNotFoundException(
                            "Applicant Type with ID "
                                    + applicantTypeId
                                    + " was not found or is inactive. "
                                    + "Please select a valid applicant type.",
                            "ERR_APPLICANT_TYPE_NOT_FOUND"
                    );
                });
    }

    @Override
    @Transactional
    public void updateDocuments(ProductDocumentMappingRequestDto request) {

        logger.info(
                "Updating document mappings. "
                        + "productId={}, applicantTypeIds={}, requiredDocumentIds={}, updatedBy={}",
                request != null ? request.getProductId() : null,
                request != null ? request.getApplicantTypeIds() : null,
                request != null ? request.getRequiredDocumentIds() : null,
                request != null ? request.getUpdatedBy() : null
        );

        validateAssignRequest(request);

        // =====================================================================
        // PRODUCT
        // =====================================================================

        Product product = productRepository
                .findByIdAndIsActiveTrueAndIsDeletedFalse(
                        request.getProductId()
                )
                .orElseThrow(() -> {

                    logger.warn(
                            "Unable to update document mappings because product "
                                    + "was not found/inactive/deleted. productId={}",
                            request.getProductId()
                    );

                    return new ResourceNotFoundException(
                            "The selected product was not found or is inactive. "
                                    + "Please select an active product.",
                            "ERR_PRODUCT_NOT_FOUND"
                    );
                });

        Long updatedBy = request.getUpdatedBy();

        // =====================================================================
        // REQUESTED DOCUMENT IDS
        // =====================================================================

        /*
         * Remove duplicate document IDs from request
         * while preserving frontend order.
         *
         * null / empty means:
         * deactivate all documents for each selected applicant type.
         */
        List<Long> requestedDocumentIds =
                request.getRequiredDocumentIds() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                        new LinkedHashSet<>(
                                request.getRequiredDocumentIds()
                        )
                );

        // =====================================================================
        // VALIDATE DOCUMENTS
        // =====================================================================

        Map<Long, ProductRequiredDocuments> documentById =
                new HashMap<>();

        if (!requestedDocumentIds.isEmpty()) {

            List<ProductRequiredDocuments> requiredDocuments =
                    requiredDocumentsRepository
                            .findAllByIdInAndIsActiveTrueAndIsDeletedFalse(
                                    requestedDocumentIds
                            );

            documentById =
                    requiredDocuments.stream()
                            .collect(
                                    Collectors.toMap(
                                            ProductRequiredDocuments::getId,
                                            document -> document
                                    )
                            );

            Set<Long> missingOrInactiveDocumentIds =
                    new LinkedHashSet<>(
                            requestedDocumentIds
                    );

            missingOrInactiveDocumentIds.removeAll(
                    documentById.keySet()
            );

            if (!missingOrInactiveDocumentIds.isEmpty()) {

                logger.warn(
                        "Unable to update document mappings because one or more "
                                + "documents were not found/inactive. productId={}, invalidDocumentIds={}",
                        product.getId(),
                        missingOrInactiveDocumentIds
                );

                throw new ResourceNotFoundException(
                        "The following required document IDs were not found or are inactive: "
                                + missingOrInactiveDocumentIds
                                + ". Please select valid active documents.",
                        "ERR_REQUIRED_DOCUMENTS_NOT_FOUND"
                );
            }
        }

        // =====================================================================
        // APPLICANT TYPES
        // =====================================================================

        /*
         * Remove duplicate applicant type IDs
         * while preserving request order.
         */
        List<Long> requestedApplicantTypeIds =
                request.getApplicantTypeIds() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                        new LinkedHashSet<>(
                                request.getApplicantTypeIds()
                        )
                );

        /*
         * Same behaviour as assign:
         *
         * no applicant types = global mapping
         */
        List<ApplicantType> applicantTypes =
                new ArrayList<>();

        if (requestedApplicantTypeIds.isEmpty()) {

            applicantTypes.add(null);

        } else {

            for (Long applicantTypeId : requestedApplicantTypeIds) {

                ApplicantType applicantType =
                        resolveApplicantType(
                                applicantTypeId
                        );

                applicantTypes.add(
                        applicantType
                );
            }
        }

        // =====================================================================
        // EXISTING MAPPINGS FOR PRODUCT
        // =====================================================================

        /*
         * Important:
         *
         * Fetch active + inactive mappings.
         *
         * We need inactive mappings too because an old mapping
         * can be reactivated instead of inserting a duplicate row.
         */
        List<ProductDocumentMapping> existingProductMappings =
                mappingRepository.findByProductId(
                        product.getId()
                );

        List<ProductDocumentMapping> mappingsToSave =
                new ArrayList<>();

        int deactivatedCount = 0;
        int reactivatedCount = 0;
        int reorderedCount = 0;
        int newMappingCount = 0;

        // =====================================================================
        // UPDATE EACH SELECTED APPLICANT TYPE
        // =====================================================================

        for (ApplicantType applicantType : applicantTypes) {

            Long applicantTypeId =
                    applicantType != null
                            ? applicantType.getId()
                            : null;

            /*
             * Existing mappings only for this exact:
             *
             * Product + Applicant Type
             *
             * Other applicant types are untouched.
             */
            Map<Long, ProductDocumentMapping> existingByDocumentId =
                    existingProductMappings
                            .stream()
                            .filter(mapping ->
                                    Objects.equals(
                                            applicantTypeId,
                                            mapping.getApplicantType() != null
                                                    ? mapping.getApplicantType().getId()
                                                    : null
                                    )
                            )
                            .collect(
                                    Collectors.toMap(
                                            mapping ->
                                                    mapping
                                                            .getRequiredDocument()
                                                            .getId(),
                                            mapping -> mapping,
                                            /*
                                             * Defensive fallback.
                                             * Database unique constraint should normally
                                             * prevent duplicate combinations.
                                             */
                                            (first, second) -> first
                                    )
                            );

            // ================================================================
            // 1. DEACTIVATE REMOVED DOCUMENTS
            // ================================================================

            for (Map.Entry<Long, ProductDocumentMapping> entry
                    : existingByDocumentId.entrySet()) {

                Long existingDocumentId =
                        entry.getKey();

                ProductDocumentMapping existingMapping =
                        entry.getValue();

                /*
                 * Existing document is not present in new request.
                 *
                 * Deactivate it only for this applicant type.
                 */
                if (!requestedDocumentIds.contains(existingDocumentId)
                        && existingMapping.isActive()) {

                    existingMapping.setActive(false);
                    existingMapping.setUpdatedBy(updatedBy);

                    mappingsToSave.add(
                            existingMapping
                    );

                    deactivatedCount++;

                    logger.debug(
                            "Document mapping deactivated. "
                                    + "mappingId={}, productId={}, applicantTypeId={}, documentId={}",
                            existingMapping.getId(),
                            product.getId(),
                            applicantTypeId,
                            existingDocumentId
                    );
                }
            }

            // ================================================================
            // 2. ADD / REACTIVATE / REORDER REQUESTED DOCUMENTS
            // ================================================================

            int displayOrder = 1;

            for (Long documentId : requestedDocumentIds) {

                ProductDocumentMapping existingMapping =
                        existingByDocumentId.get(
                                documentId
                        );

                if (existingMapping != null) {

                    boolean wasInactive =
                            !existingMapping.isActive();

                    boolean orderChanged =
                            existingMapping.getDisplayOrder() == null
                                    || !Objects.equals(
                                    existingMapping.getDisplayOrder(),
                                    displayOrder
                            );

                    existingMapping.setActive(true);

                    existingMapping.setDisplayOrder(
                            displayOrder
                    );

                    existingMapping.setMandatory(true);

                    existingMapping.setUpdatedBy(
                            updatedBy
                    );

                    mappingsToSave.add(
                            existingMapping
                    );

                    if (wasInactive) {
                        reactivatedCount++;
                    }

                    if (orderChanged) {
                        reorderedCount++;
                    }

                    logger.debug(
                            "Existing document mapping updated. "
                                    + "mappingId={}, productId={}, applicantTypeId={}, "
                                    + "documentId={}, displayOrder={}, reactivated={}",
                            existingMapping.getId(),
                            product.getId(),
                            applicantTypeId,
                            documentId,
                            displayOrder,
                            wasInactive
                    );

                } else {

                    ProductRequiredDocuments document =
                            documentById.get(
                                    documentId
                            );

                    /*
                     * This should always exist because all requested
                     * documents were validated above.
                     */
                    if (document == null) {

                        logger.error(
                                "Validated document unexpectedly missing from lookup map. "
                                        + "productId={}, applicantTypeId={}, documentId={}",
                                product.getId(),
                                applicantTypeId,
                                documentId
                        );

                        throw new ResourceNotFoundException(
                                "Required document with ID "
                                        + documentId
                                        + " was not found.",
                                "ERR_REQUIRED_DOCUMENT_NOT_FOUND"
                        );
                    }

                    ProductDocumentMapping newMapping =
                            new ProductDocumentMapping();

                    newMapping.setProduct(
                            product
                    );

                    newMapping.setRequiredDocument(
                            document
                    );

                    newMapping.setApplicantType(
                            applicantType
                    );

                    newMapping.setMandatory(
                            true
                    );

                    newMapping.setDisplayOrder(
                            displayOrder
                    );

                    newMapping.setActive(
                            true
                    );

                    newMapping.setCreatedBy(
                            updatedBy
                    );

                    newMapping.setUpdatedBy(
                            updatedBy
                    );

                    mappingsToSave.add(
                            newMapping
                    );

                    newMappingCount++;

                    logger.debug(
                            "New document mapping prepared during update. "
                                    + "productId={}, applicantTypeId={}, documentId={}, displayOrder={}",
                            product.getId(),
                            applicantTypeId,
                            documentId,
                            displayOrder
                    );
                }

                displayOrder++;
            }
        }

        // =====================================================================
        // SAVE
        // =====================================================================

        try {

            if (!mappingsToSave.isEmpty()) {

                mappingRepository.saveAllAndFlush(
                        mappingsToSave
                );
            }

        } catch (DataIntegrityViolationException ex) {

            logger.error(
                    "Database constraint violation while updating document mappings. "
                            + "productId={}, applicantTypeIds={}, documentIds={}",
                    product.getId(),
                    requestedApplicantTypeIds,
                    requestedDocumentIds,
                    ex
            );

            throw new ValidationException(
                    "Unable to update document mappings because one or more "
                            + "document/applicant type combinations conflict with existing data. "
                            + "Please refresh and try again.",
                    "ERR_DOCUMENT_MAPPING_UPDATE_CONFLICT"
            );
        }

        logger.info(
                "Document mappings updated successfully. "
                        + "productId={}, applicantTypeIds={}, documentIds={}, "
                        + "changedMappings={}, newMappings={}, deactivated={}, "
                        + "reactivated={}, reordered={}",
                product.getId(),
                requestedApplicantTypeIds,
                requestedDocumentIds,
                mappingsToSave.size(),
                newMappingCount,
                deactivatedCount,
                reactivatedCount,
                reorderedCount
        );
    }






}