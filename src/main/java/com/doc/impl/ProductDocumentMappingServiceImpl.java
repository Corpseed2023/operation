package com.doc.impl;

import com.doc.dto.document.*;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDocumentMappingServiceImpl implements ProductDocumentMappingService {

    private final ProductDocumentMappingRepository mappingRepository;
    private final ProductRequiredDocumentsRepository requiredDocumentsRepository;
    private final ApplicantTypeRepository applicantTypeRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void assignDocuments(ProductDocumentMappingRequestDto request) {
        log.info("Assigning documents to product ID: {}, applicantType ID: {}, updatedBy: {}",
                request.getProductId(), request.getApplicantTypeId(), request.getUpdatedBy());

        validateAssignRequest(request);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + request.getProductId(), "ERR_PRODUCT_NOT_FOUND"));

        ApplicantType applicantType = resolveApplicantType(request.getApplicantTypeId());

        Long updatedBy = request.getUpdatedBy();



        // If no documents requested → just cleared old ones
        if (request.getRequiredDocumentIds() == null || request.getRequiredDocumentIds().isEmpty()) {
            log.info("No new documents assigned. Cleared existing mappings only.");
            return;
        }

        // Step 2: Fetch all requested required documents in one query
        List<ProductRequiredDocuments> requiredDocs = requiredDocumentsRepository
                .findAllByIdInAndIsActiveTrueAndIsDeletedFalse(request.getRequiredDocumentIds());

        Map<Long, ProductRequiredDocuments> docById = requiredDocs.stream()
                .collect(Collectors.toMap(ProductRequiredDocuments::getId, doc -> doc));

        // Validate all requested document IDs exist and are active
        Set<Long> missingOrInactiveIds = request.getRequiredDocumentIds().stream()
                .filter(id -> !docById.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingOrInactiveIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "One or more required documents not found or inactive: " + missingOrInactiveIds,
                    "ERR_REQUIRED_DOCUMENTS_NOT_FOUND");
        }

        // Step 3: Create new mappings with proper order
        List<ProductDocumentMapping> newMappings = new ArrayList<>();
        for (int order = 0; order < request.getRequiredDocumentIds().size(); order++) {
            Long docId = request.getRequiredDocumentIds().get(order);
            ProductRequiredDocuments doc = docById.get(docId);

            ProductDocumentMapping mapping = new ProductDocumentMapping();
            mapping.setProduct(product);
            mapping.setRequiredDocument(doc);
            mapping.setApplicantType(applicantType);
            mapping.setMandatory(true);
            mapping.setDisplayOrder(order + 1);
            mapping.setActive(true);
            mapping.setCreatedBy(updatedBy);
            mapping.setUpdatedBy(updatedBy);

            newMappings.add(mapping);
        }

        mappingRepository.saveAll(newMappings);
        log.info("Successfully assigned {} documents to product ID: {}", newMappings.size(), product.getId());
    }

    @Override
    public List<ProductDocumentMappingResponseDto> getRequiredDocuments(Long productId, Long applicantTypeId) {
        log.debug("Fetching required documents for product ID: {}, applicantType ID: {}", productId, applicantTypeId);

        validateProductExists(productId);

        List<ProductDocumentMapping> mappings = mappingRepository
                .findByProductIdAndApplicantTypeIdAndIsActiveTrue(productId, applicantTypeId);

        return mappings.stream()
                .sorted(Comparator.comparingInt(m ->
                        m.getDisplayOrder() != null ? m.getDisplayOrder() : Integer.MAX_VALUE))
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<ProductDocumentMappingGroupedDto> getAllMappingsGroupedByApplicantType(Long productId) {
        log.debug("Fetching all document mappings grouped by applicant type for product ID: {}", productId);

        validateProductExists(productId);

        // Global (common) documents
        List<ProductDocumentMapping> globalMappings = mappingRepository
                .findByProductIdAndApplicantTypeIsNullAndIsActiveTrue(productId);

        // Applicant-type specific
        List<ProductDocumentMapping> typedMappings = mappingRepository
                .findByProductIdAndApplicantTypeIsNotNullAndIsActiveTrue(productId);

        Map<Long, List<ProductDocumentMapping>> groupedByApplicantType = typedMappings.stream()
                .collect(Collectors.groupingBy(m -> m.getApplicantType().getId()));

        List<ProductDocumentMappingGroupedDto> result = new ArrayList<>();

        // Add Common Documents group
        ProductDocumentMappingGroupedDto commonGroup = new ProductDocumentMappingGroupedDto();
        commonGroup.setApplicantTypeId(null);
        commonGroup.setApplicantTypeName("Common Documents");
        commonGroup.setDocuments(globalMappings.stream()
                .sorted(Comparator.comparingInt(m -> Optional.ofNullable(m.getDisplayOrder()).orElse(0)))
                .map(this::mapToResponseDto)
                .toList());
        result.add(commonGroup);

        // Add each applicant type group (even if empty — optional)
        applicantTypeRepository.findAllByIsActiveTrueAndIsDeletedFalse()
                .forEach(applicantType -> {
                    List<ProductDocumentMapping> docs = groupedByApplicantType.getOrDefault(applicantType.getId(), List.of());

                    ProductDocumentMappingGroupedDto group = new ProductDocumentMappingGroupedDto();
                    group.setApplicantTypeId(applicantType.getId());
                    group.setApplicantTypeName(applicantType.getName());
                    group.setDocuments(docs.stream()
                            .sorted(Comparator.comparingInt(m -> Optional.ofNullable(m.getDisplayOrder()).orElse(0)))
                            .map(this::mapToResponseDto)
                            .toList());
                    result.add(group);
                });

        return result;
    }

    private ProductDocumentMappingResponseDto mapToResponseDto(ProductDocumentMapping mapping) {
        ProductRequiredDocuments doc = mapping.getRequiredDocument();
        return new ProductDocumentMappingResponseDto(
                mapping.getId(),
                doc.getId(),
                doc.getName(),
                doc.getType(),
                doc.getDescription(),
                mapping.isMandatory(),
                mapping.getDisplayOrder(),
                doc.getAllowedFormats(),
                doc.getExpiryType(),
                doc.getMaxValidityYears()
        );
    }

    private void validateAssignRequest(ProductDocumentMappingRequestDto request) {
        if (request.getProductId() == null) {
            throw new ValidationException("Product ID is required", "ERR_MISSING_PRODUCT_ID");
        }
        if (request.getUpdatedBy() == null) {
            throw new ValidationException("UpdatedBy user ID is required", "ERR_MISSING_UPDATED_BY");
        }
        if (request.getRequiredDocumentIds() != null && request.getRequiredDocumentIds().contains(null)) {
            throw new ValidationException("Required document IDs cannot contain null values", "ERR_INVALID_DOCUMENT_IDS");
        }
    }

    private void validateProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with ID: " + productId, "ERR_PRODUCT_NOT_FOUND");
        }
    }

    private ApplicantType resolveApplicantType(Long applicantTypeId) {
        if (applicantTypeId == null) {
            return null; // Global mapping
        }
        return applicantTypeRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(applicantTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Applicant Type not found with ID: " + applicantTypeId, "ERR_APPLICANT_TYPE_NOT_FOUND"));
    }

    @Override
    public List<ProductDocumentMappingResponseDto> getFinalRequiredDocuments(Long productId, Long applicantTypeId) {
        log.debug("Fetching final required documents for productId: {}, applicantTypeId: {}", productId, applicantTypeId);

        validateProductExists(productId);

        ApplicantType applicantType = resolveApplicantType(applicantTypeId); // will throw if invalid

        // 1. Get global documents (common to all applicant types)
        List<ProductDocumentMapping> global = mappingRepository
                .findByProductIdAndApplicantTypeIsNullAndIsActiveTrue(productId);

        // 2. Get specific documents for this applicant type
        List<ProductDocumentMapping> specific = mappingRepository
                .findByProductIdAndApplicantTypeIdAndIsActiveTrue(productId, applicantTypeId);

        // 3. Merge: specific overrides global if same document exists
        Map<Long, ProductDocumentMapping> finalMap = new LinkedHashMap<>();

        // First: add all global
        global.forEach(m -> finalMap.put(m.getRequiredDocument().getId(), m));

        // Then: override with specific (higher priority)
        specific.forEach(m -> finalMap.put(m.getRequiredDocument().getId(), m));

        // 4. Sort by displayOrder (specific wins), fallback to global order
        return finalMap.values().stream()
                .sorted(Comparator.comparingInt(m ->
                        Optional.ofNullable(m.getDisplayOrder()).orElse(Integer.MAX_VALUE)))
                .map(this::mapToResponseDto)
                .toList();
    }




    @Override
    public ProductDocumentRequirementResponseDto getDocumentRequirementsGrouped(Long productId) {
        log.info("Fetching document requirements grouped for product ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + productId, "ERR_PRODUCT_NOT_FOUND"));

        List<ApplicantTypeDocumentGroupDto> groups = new ArrayList<>();

        // Check if this product has ANY applicant-type-specific mappings
        boolean hasSpecificMappings = mappingRepository
                .existsByProductIdAndApplicantTypeIsNotNullAndIsActiveTrue(productId);

        if (hasSpecificMappings) {
            // Case 1: Product has applicant-type-specific documents → group by applicant type
            List<ApplicantType> applicantTypes = applicantTypeRepository
                    .findAllByIsActiveTrueAndIsDeletedFalseOrderByNameAsc();

            for (ApplicantType applicantType : applicantTypes) {
                List<ProductDocumentMappingResponseDto> docs =
                        getFinalRequiredDocuments(productId, applicantType.getId());

                if (!docs.isEmpty()) {
                    groups.add(new ApplicantTypeDocumentGroupDto(
                            applicantType.getId(),
                            applicantType.getName(),
                            docs
                    ));
                }
            }
        } else {
            // Case 2: No applicant-type-specific mappings → return single group with null applicantType
            List<ProductDocumentMappingResponseDto> globalDocs =
                    getFinalRequiredDocuments(productId, null);

            if (!globalDocs.isEmpty()) {
                groups.add(new ApplicantTypeDocumentGroupDto(
                        null,           // applicantTypeId = null
                        null,           // applicantTypeName = null (exactly as you want)
                        globalDocs
                ));
            }
        }

        return new ProductDocumentRequirementResponseDto(
                product.getId(),
                product.getProductName(),
                groups
        );
    }



}