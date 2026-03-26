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

        Product product = productRepository.findActiveUserById(request.getProductId())
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
    @Transactional(readOnly = true)
    public List<ProductDocumentMappingResponseDto> getRequiredDocuments(Long productId, Long applicantTypeId) {
        log.debug("Fetching required documents for product={}, applicantType={}", productId, applicantTypeId);
        validateProductExists(productId);

        List<ProductDocumentMapping> mappings;
        if (applicantTypeId == null || applicantTypeId == -1) {
            mappings = mappingRepository.findByProductIdAndIsActiveTrue(productId);
        } else {
            mappings = mappingRepository.findByProductIdAndApplicantTypeIdAndIsActiveTrue(productId, applicantTypeId);
        }

        return mappings.stream()
                .sorted(Comparator.comparingInt(m -> m.getDisplayOrder() != null ? m.getDisplayOrder() : Integer.MAX_VALUE))
                .map(this::mapToResponseDtoInDocs)
                .toList();
    }

    private ProductDocumentMappingResponseDto mapToResponseDtoInDocs(ProductDocumentMapping mapping) {

        ProductRequiredDocuments doc = mapping.getRequiredDocument();
        ApplicantType applicantType = mapping.getApplicantType(); // get it from mapping

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
                doc.getMaxValidityYears(),

                //   new fields
                applicantType != null ? applicantType.getId() : null,
                applicantType != null ? applicantType.getName() : null
        );
    }




//    private ProductDocumentMappingResponseDto mapToResponseDto(ProductDocumentMapping mapping) {
//        ProductRequiredDocuments doc = mapping.getRequiredDocument();
//        return new ProductDocumentMappingResponseDto(
//                mapping.getId(),
//                doc.getId(),
//                doc.getName(),
//                doc.getType(),
//                doc.getDescription(),
//                mapping.isMandatory(),
//                mapping.getDisplayOrder(),
//                doc.getAllowedFormats(),
//                doc.getExpiryType(),
//                doc.getMaxValidityYears());
//    }

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
            return null;
        }
        return applicantTypeRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(applicantTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Applicant Type not found with ID: " + applicantTypeId, "ERR_APPLICANT_TYPE_NOT_FOUND"));
    }




}