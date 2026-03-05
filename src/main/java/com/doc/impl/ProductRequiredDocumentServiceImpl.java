    // src/main/java/com/doc/impl/ProductRequiredDocumentServiceImpl.java
    package com.doc.impl;

    import com.doc.dto.document.ProductRequiredDocumentRequestDto;
    import com.doc.dto.document.ProductRequiredDocumentResponseDto;
    import com.doc.entity.document.ProductRequiredDocuments;
    import com.doc.exception.ResourceNotFoundException;
    import com.doc.exception.ValidationException;
    import com.doc.repository.documentRepo.ProductRequiredDocumentRepository;
    import com.doc.service.ProductRequiredDocumentService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.domain.*;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.List;

    @Service
    @Transactional
    public class ProductRequiredDocumentServiceImpl implements ProductRequiredDocumentService {

        private final ProductRequiredDocumentRepository productRequiredDocumentRepository;

        @Autowired
        public ProductRequiredDocumentServiceImpl(ProductRequiredDocumentRepository productRequiredDocumentRepository) {
            this.productRequiredDocumentRepository = productRequiredDocumentRepository;
        }

        @Override
        public ProductRequiredDocumentResponseDto create(ProductRequiredDocumentRequestDto dto) {
            validateUniqueConstraint(dto, null);

            ProductRequiredDocuments entity = new ProductRequiredDocuments();
            mapDtoToEntity(dto, entity);
            entity.setActive(true);
            entity.setDeleted(false);
            entity.setCreatedBy(dto.getCreatedBy());
            entity.setUpdatedBy(dto.getUpdatedBy());

            entity = productRequiredDocumentRepository.save(entity);
            return mapToResponseDto(entity);
        }

        @Override
        public ProductRequiredDocumentResponseDto update(Long id, ProductRequiredDocumentRequestDto dto) {
            ProductRequiredDocuments entity = findActiveById(id);

            validateUniqueConstraint(dto, id);

            mapDtoToEntity(dto, entity);
            entity.setUpdatedBy(dto.getUpdatedBy());

            entity = productRequiredDocumentRepository.save(entity);
            return mapToResponseDto(entity);
        }



        @Override
        public List<ProductRequiredDocumentResponseDto> getActivePaginated(int page, int size) {
            page = Math.max(page, 1);
            size = size > 0 ? size : 20;

            Pageable pageable = PageRequest.of(page - 1, size, Sort.by("name").ascending());
            return productRequiredDocumentRepository.findAllByIsDeletedFalseAndIsActiveTrue(pageable)  // FIXED
                    .getContent()
                    .stream()
                    .map(this::mapToResponseDto)
                    .toList();
        }

        private ProductRequiredDocuments findActiveById(Long id) {
            return productRequiredDocumentRepository.findByIdAndIsDeletedFalse(id)  // FIXED
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Required document template not found with ID: " + id,
                            "ERR_REQ_DOC_NOT_FOUND"));
        }

        // Unique constraint validation
        private void validateUniqueConstraint(ProductRequiredDocumentRequestDto dto, Long excludeId) {
            String name = dto.getName() != null ? dto.getName().trim() : "";
            String country = dto.getCountry() != null ? dto.getCountry().trim() : "";
            String centralName = dto.getCentralName() != null ? dto.getCentralName().trim() : "";
            String stateName = dto.getStateName() != null ? dto.getStateName().trim() : "";

            boolean exists = excludeId == null
                    ? productRequiredDocumentRepository.existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalse(
                    name, country, centralName, stateName)
                    : productRequiredDocumentRepository.existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalseAndIdNot(
                    name, country, centralName, stateName, excludeId);

            if (exists) {
                throw new ValidationException(
                        "A document template with the same name and location already exists.",
                        "ERR_DUPLICATE_REQ_DOCUMENT");
            }
        }

        private void mapDtoToEntity(ProductRequiredDocumentRequestDto dto, ProductRequiredDocuments entity) {
            entity.setName(dto.getName().trim());
            entity.setDescription(dto.getDescription());
            entity.setType(dto.getType());
            entity.setCountry(dto.getCountry() != null ? dto.getCountry().trim() : "");
            entity.setCentralName(dto.getCentralName() != null ? dto.getCentralName().trim() : "");
            entity.setStateName(dto.getStateName() != null ? dto.getStateName().trim() : "");
            entity.setExpiryType(dto.getExpiryType());
            entity.setMandatory(dto.isMandatory());
            entity.setMaxValidityYears(dto.getMaxValidityYears());
            entity.setMinFileSizeKb(dto.getMinFileSizeKb());
            entity.setAllowedFormats(dto.getAllowedFormats());
        }

        private ProductRequiredDocumentResponseDto mapToResponseDto(ProductRequiredDocuments entity) {
            ProductRequiredDocumentResponseDto dto = new ProductRequiredDocumentResponseDto();

            dto.setId(entity.getId());
            dto.setName(entity.getName());
            dto.setDescription(entity.getDescription());
            dto.setType(entity.getType());
            dto.setCountry(entity.getCountry());
            dto.setCentralName(entity.getCentralName());
            dto.setStateName(entity.getStateName());
            dto.setExpiryType(entity.getExpiryType());
            dto.setMandatory(entity.isMandatory());
            dto.setMaxValidityYears(entity.getMaxValidityYears());
            dto.setMinFileSizeKb(entity.getMinFileSizeKb());
            dto.setAllowedFormats(entity.getAllowedFormats());
            dto.setCreatedBy(entity.getCreatedBy());
            dto.setUpdatedBy(entity.getUpdatedBy());
            dto.setCreatedDate(entity.getCreatedDate());
            dto.setUpdatedDate(entity.getUpdatedDate());
            dto.setActive(entity.isActive());

            return dto;
        }
    }