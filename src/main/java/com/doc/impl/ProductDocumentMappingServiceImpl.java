package com.doc.impl;

import com.doc.dto.document.*;
import com.doc.entity.document.ApplicantType;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.product.Product;
import com.doc.repository.*;
import com.doc.repository.documentRepo.ApplicantTypeRepository;
import com.doc.repository.documentRepo.ProductRequiredDocumentsRepository;
import com.doc.service.ProductDocumentMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDocumentMappingServiceImpl implements ProductDocumentMappingService {

    private final ProductDocumentMappingRepository mappingRepository;
    private final ProductRepository productRepository;
    private final ProductRequiredDocumentsRepository requiredDocumentsRepository;
    private final ApplicantTypeRepository applicantTypeRepository;

    @Override
    @Transactional
    public void assignDocuments(ProductDocumentMappingRequestDto request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + request.getProductId()));

        ApplicantType applicantType = null;
        if (request.getApplicantTypeId() != null) {
            applicantType = applicantTypeRepository.findById(request.getApplicantTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("ApplicantType not found with id: " + request.getApplicantTypeId()));
        }

        Long updatedBy = Optional.ofNullable(request.getUpdatedBy())
                .orElseThrow(() -> new IllegalArgumentException("updatedBy is required"));

        // Step 1: Delete all existing mappings for this product + applicantType (null means global)
        mappingRepository.deleteByProductIdAndApplicantTypeId(
                request.getProductId(),
                request.getApplicantTypeId() // can be null
        );

        if (request.getRequiredDocumentIds() == null || request.getRequiredDocumentIds().isEmpty()) {
            return; // Just cleared old ones
        }

        // Step 2: Fetch all required documents in one query
        List<ProductRequiredDocuments> requiredDocs = requiredDocumentsRepository
                .findAllByIdInAndIsActiveTrueAndIsDeletedFalse(request.getRequiredDocumentIds());

        Map<Long, ProductRequiredDocuments> docMap = requiredDocs.stream()
                .collect(Collectors.toMap(ProductRequiredDocuments::getId, d -> d));

        // Validate all requested IDs exist
        Set<Long> missingIds = request.getRequiredDocumentIds().stream()
                .filter(id -> !docMap.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            throw new EntityNotFoundException("Required documents not found: " + missingIds);
        }

        // Step 3: Create new mappings
        List<ProductDocumentMapping> newMappings = new ArrayList<>();
        for (int i = 0; i < request.getRequiredDocumentIds().size(); i++) {
            Long docId = request.getRequiredDocumentIds().get(i);
            ProductRequiredDocuments doc = docMap.get(docId);

            ProductDocumentMapping mapping = new ProductDocumentMapping();
            mapping.setProduct(product);
            mapping.setRequiredDocument(doc);
            mapping.setApplicantType(applicantType);
            mapping.setMandatory(true); // or make configurable later
            mapping.setDisplayOrder(i + 1);
            mapping.setActive(true);
            mapping.setCreatedBy(updatedBy);
            mapping.setUpdatedBy(updatedBy);

            newMappings.add(mapping);
        }

        mappingRepository.saveAll(newMappings);
    }

    @Override
    public List<ProductDocumentMappingResponseDto> getRequiredDocuments(Long productId, Long applicantTypeId) {
        List<ProductDocumentMapping> mappings = mappingRepository
                .findByProductIdAndApplicantTypeIdAndIsActiveTrue(productId, applicantTypeId);

        return mappings.stream()
                .sorted(Comparator.comparingInt(m -> m.getDisplayOrder() != null ? m.getDisplayOrder() : Integer.MAX_VALUE))
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<ProductDocumentMappingGroupedDto> getAllMappingsGroupedByApplicantType(Long productId) {
        // First get global (applicantType = null)
        List<ProductDocumentMapping> globalMappings = mappingRepository
                .findByProductIdAndApplicantTypeIsNullAndIsActiveTrue(productId);

        // Then get per applicant type
        List<ProductDocumentMapping> typedMappings = mappingRepository
                .findByProductIdAndApplicantTypeIsNotNullAndIsActiveTrue(productId);

        Map<Long, List<ProductDocumentMapping>> grouped = typedMappings.stream()
                .collect(Collectors.groupingBy(m -> m.getApplicantType().getId()));

        List<ProductDocumentMappingGroupedDto> result = new ArrayList<>();

        // Add global documents
        ProductDocumentMappingGroupedDto globalGroup = new ProductDocumentMappingGroupedDto();
        globalGroup.setApplicantTypeId(null);
        globalGroup.setApplicantTypeName("Common Documents");
        globalGroup.setDocuments(globalMappings.stream()
                .sorted(Comparator.comparingInt(m -> m.getDisplayOrder() != null ? m.getDisplayOrder() : 0))
                .map(this::mapToResponseDto)
                .toList());
        result.add(globalGroup);

        // Add applicant-type specific groups
        applicantTypeRepository.findAllByIsActiveTrueAndIsDeletedFalse().forEach(at -> {
            List<ProductDocumentMapping> docs = grouped.getOrDefault(at.getId(), Collections.emptyList());
            if (!docs.isEmpty() || true) { // always include even if empty? optional
                ProductDocumentMappingGroupedDto group = new ProductDocumentMappingGroupedDto();
                group.setApplicantTypeId(at.getId());
                group.setApplicantTypeName(at.getName());
                group.setDocuments(docs.stream()
                        .sorted(Comparator.comparingInt(m -> m.getDisplayOrder() != null ? m.getDisplayOrder() : 0))
                        .map(this::mapToResponseDto)
                        .toList());
                result.add(group);
            }
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
}