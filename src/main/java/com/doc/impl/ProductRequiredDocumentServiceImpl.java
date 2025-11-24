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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductRequiredDocumentServiceImpl implements ProductRequiredDocumentService {

    @Autowired
    private ProductRequiredDocumentRepository productRequiredDocumentRepository;

    @Override
    public ProductRequiredDocumentResponseDto create(ProductRequiredDocumentRequestDto dto) {
        validateUniqueConstraint(dto.getName(), dto.getCountry(), dto.getCentralName(), dto.getStateName(), null);

        ProductRequiredDocuments entity = new ProductRequiredDocuments();
        mapDtoToEntity(dto, entity);
        entity.setActive(true);
        entity.setDeleted(false);

        entity = productRequiredDocumentRepository.save(entity);
        return mapToResponseDto(entity);
    }

    @Override
    public ProductRequiredDocumentResponseDto update(Long id, ProductRequiredDocumentRequestDto dto) {
        ProductRequiredDocuments entity = productRequiredDocumentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Required document not found", "ERR_REQ_DOC_NOT_FOUND"));

        validateUniqueConstraint(dto.getName(), dto.getCountry(), dto.getCentralName(), dto.getStateName(), id);

        mapDtoToEntity(dto, entity);
        entity = productRequiredDocumentRepository.save(entity);
        return mapToResponseDto(entity);
    }

    @Override
    public void softDelete(Long id) {
        ProductRequiredDocuments entity = productRequiredDocumentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Required document not found", "ERR_REQ_DOC_NOT_FOUND"));
        entity.setDeleted(true);
        entity.setActive(false);
        productRequiredDocumentRepository.save(entity);
    }

    @Override
    public ProductRequiredDocumentResponseDto getById(Long id) {
        return productRequiredDocumentRepository.findByIdAndIsDeletedFalse(id)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Required document not found", "ERR_REQ_DOC_NOT_FOUND"));
    }

    @Override
    public Page<ProductRequiredDocumentResponseDto> getAllPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return productRequiredDocumentRepository.findAllByIsDeletedFalse(pageable).map(this::mapToResponseDto);
    }

    @Override
    public Page<ProductRequiredDocumentResponseDto> getAllActivePaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return productRequiredDocumentRepository.findAllActive(pageable).map(this::mapToResponseDto);
    }

    @Override
    public List<ProductRequiredDocumentResponseDto> getAllActive() {
        Pageable pageable = PageRequest.of(0, 1000, Sort.by("name"));
        return productRequiredDocumentRepository.findAllActive(pageable)
                .getContent()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private void validateUniqueConstraint(String name, String country, String centralName, String stateName, Long excludeId) {
        boolean exists = (excludeId == null)
                ? productRequiredDocumentRepository.existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalse(name, country, centralName, stateName)
                : productRequiredDocumentRepository.existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalseAndIdNot(name, country, centralName, stateName, excludeId);

        if (exists) {
            throw new ValidationException("A required document with this name and location combination already exists", "ERR_DUPLICATE_REQ_DOCUMENT");
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
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setUpdatedBy(dto.getUpdatedBy());
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