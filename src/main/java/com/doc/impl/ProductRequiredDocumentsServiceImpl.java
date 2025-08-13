package com.doc.impl;

import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsRequestDto;
import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsResponseDto;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductRequiredDocuments;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProductRepository;
import com.doc.repository.ProductRequiredDocumentsRepository;
import com.doc.repository.UserRepository;
import com.doc.service.ProductRequiredDocumentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductRequiredDocumentsServiceImpl implements ProductRequiredDocumentsService {

    @Autowired
    private ProductRequiredDocumentsRepository requiredDocumentsRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<ProductRequiredDocumentsResponseDto> createRequiredDocuments(List<ProductRequiredDocumentsRequestDto> requestDtoList) {
        List<ProductRequiredDocumentsResponseDto> responseList = new ArrayList<>();

        for (ProductRequiredDocumentsRequestDto requestDto : requestDtoList) {
            validateRequestDto(requestDto);

            // Check for duplicate document name
            if (requiredDocumentsRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
                throw new ValidationException("Required document with name " + requestDto.getName() + " already exists");
            }

            // Validate createdBy and updatedBy users
            User createdBy = userRepository.findActiveUserById(requestDto.getCreatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getCreatedBy() + " not found"));

            User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getUpdatedBy() + " not found"));

            // Validate product IDs
            List<Product> products = new ArrayList<>();
            if (requestDto.getProductIds() != null && !requestDto.getProductIds().isEmpty()) {
                products = productRepository.findAllById(requestDto.getProductIds())
                        .stream()
                        .filter(p -> !p.isDeleted())
                        .collect(Collectors.toList());
                if (products.size() != requestDto.getProductIds().size()) {
                    throw new ResourceNotFoundException("One or more products not found");
                }
            }

            // Create new document
            ProductRequiredDocuments document = new ProductRequiredDocuments();
            mapRequestDtoToEntity(document, requestDto);
            document.setCreatedDate(new Date());
            document.setUpdatedDate(new Date());
            document.setDeleted(false);
            document.setProducts(products);

            // Update products to include this document
            for (Product product : products) {
                product.getRequiredDocuments().add(document);
            }

            // Save document
            document = requiredDocumentsRepository.save(document);
            productRepository.saveAll(products);

            responseList.add(mapToResponseDto(document));
        }

        return responseList;
    }

    @Override
    public ProductRequiredDocumentsResponseDto getRequiredDocumentById(Long id) {
        ProductRequiredDocuments document = requiredDocumentsRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Required document with ID " + id + " not found"));
        return mapToResponseDto(document);
    }

    @Override
    public List<ProductRequiredDocumentsResponseDto> getAllRequiredDocuments(int page, int size, String name, String type, String country, String centralName, String stateName) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<ProductRequiredDocuments> documentPage;

        if (name != null || type != null || country != null || centralName != null || stateName != null) {
            documentPage = requiredDocumentsRepository.findByFilters(name, type, country, centralName, stateName, pageable);
        } else {
            documentPage = requiredDocumentsRepository.findByIsDeletedFalse(pageable);
        }

        return documentPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductRequiredDocumentsResponseDto updateRequiredDocument(Long id, ProductRequiredDocumentsRequestDto requestDto) {
        validateRequestDto(requestDto);

        // Find existing document
        ProductRequiredDocuments document = requiredDocumentsRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Required document with ID " + id + " not found"));

        // Check for duplicate document name
        if (!document.getName().equals(requestDto.getName().trim()) &&
                requiredDocumentsRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
            throw new ValidationException("Required document with name " + requestDto.getName() + " already exists");
        }

        // Validate updatedBy user
        User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getUpdatedBy() + " not found"));

        // Validate product IDs
        List<Product> products = new ArrayList<>();
        if (requestDto.getProductIds() != null && !requestDto.getProductIds().isEmpty()) {
            products = productRepository.findAllById(requestDto.getProductIds())
                    .stream()
                    .filter(p -> !p.isDeleted())
                    .collect(Collectors.toList());
            if (products.size() != requestDto.getProductIds().size()) {
                throw new ResourceNotFoundException("One or more products not found");
            }
        }

        // Clear existing product mappings
        for (Product product : document.getProducts()) {
            product.getRequiredDocuments().remove(document);
        }
        productRepository.saveAll(document.getProducts());

        // Update document fields
        mapRequestDtoToEntity(document, requestDto);
        document.setUpdatedDate(new Date());
        document.setProducts(products);

        // Update products to include this document
        for (Product product : products) {
            product.getRequiredDocuments().add(document);
        }

        // Save updated document and products
        document = requiredDocumentsRepository.save(document);
        productRepository.saveAll(products);

        return mapToResponseDto(document);
    }

    @Override
    public void deleteRequiredDocument(Long id) {
        ProductRequiredDocuments document = requiredDocumentsRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Required document with ID " + id + " not found"));

        // Remove document from associated products
        for (Product product : document.getProducts()) {
            product.getRequiredDocuments().remove(document);
        }
        productRepository.saveAll(document.getProducts());

        // Soft delete the document
        document.setDeleted(true);
        document.setUpdatedDate(new Date());
        requiredDocumentsRepository.save(document);
    }

    private void validateRequestDto(ProductRequiredDocumentsRequestDto requestDto) {
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new ValidationException("Document name cannot be empty");
        }
        if (requestDto.getType() == null || requestDto.getType().trim().isEmpty()) {
            throw new ValidationException("Document type cannot be empty");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null");
        }
        if (requestDto.getUpdatedBy() == null) {
            throw new ValidationException("Updated by user ID cannot be null");
        }
    }

    private void mapRequestDtoToEntity(ProductRequiredDocuments document, ProductRequiredDocumentsRequestDto requestDto) {
        document.setName(requestDto.getName().trim());
        document.setDescription(requestDto.getDescription());
        document.setType(requestDto.getType().trim());
        document.setCountry(requestDto.getCountry());
        document.setCentralName(requestDto.getCentralName());
        document.setStateName(requestDto.getStateName());
    }

    private ProductRequiredDocumentsResponseDto mapToResponseDto(ProductRequiredDocuments document) {
        ProductRequiredDocumentsResponseDto dto = new ProductRequiredDocumentsResponseDto();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setDescription(document.getDescription());
        dto.setType(document.getType());
        dto.setCountry(document.getCountry());
        dto.setCentralName(document.getCentralName());
        dto.setStateName(document.getStateName());
        dto.setCreatedDate(document.getCreatedDate());
        dto.setUpdatedDate(document.getUpdatedDate());

        // Set product IDs
        List<Long> productIds = document.getProducts()
                .stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        dto.setProductIds(productIds);

        return dto;
    }
}