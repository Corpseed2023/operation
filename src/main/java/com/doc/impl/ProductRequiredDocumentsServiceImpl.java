package com.doc.impl;

import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsRequestDto;
import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsResponseDto;
import com.doc.dto.project.DocumentResponseDto;
import com.doc.entity.product.Product;
import com.doc.entity.product.ProductRequiredDocuments;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectDocumentUpload;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProductRepository;
import com.doc.repository.ProductRequiredDocumentsRepository;
import com.doc.repository.ProjectDocumentUploadRepository;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.service.ProductRequiredDocumentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectDocumentUploadRepository projectDocumentUploadRepository; // Added for fetching uploads

    @Override
    public List<ProductRequiredDocumentsResponseDto> createRequiredDocuments(List<ProductRequiredDocumentsRequestDto> requestDtoList) {
        List<ProductRequiredDocumentsResponseDto> responseList = new ArrayList<>();

        for (ProductRequiredDocumentsRequestDto requestDto : requestDtoList) {
            validateRequestDto(requestDto);

            String name = requestDto.getName().trim();
            String country = Optional.ofNullable(requestDto.getCountry()).orElse("");
            String centralName = Optional.ofNullable(requestDto.getCentralName()).orElse("");
            String stateName = Optional.ofNullable(requestDto.getStateName()).orElse("");

            if (requiredDocumentsRepository.existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalse(name, country, centralName, stateName)) {
                throw new ValidationException("Required document with name " + name + " already exists for this region");
            }

            User createdBy = userRepository.findActiveUserById(requestDto.getCreatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getCreatedBy() + " not found"));
            User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getUpdatedBy() + " not found"));

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

            ProductRequiredDocuments document = new ProductRequiredDocuments();

            // **Set the ID explicitly from DTO**
            if (requestDto.getId() == null) {
                throw new ValidationException("ID must be provided for the required document");
            }
            document.setId(requestDto.getId());

            mapRequestDtoToEntity(document, requestDto);
            document.setCreatedBy(requestDto.getCreatedBy());
            document.setUpdatedBy(requestDto.getUpdatedBy());
            document.setCreatedDate(new Date());
            document.setUpdatedDate(new Date());
            document.setDeleted(false);
            document.setProducts(products);
            document.setUuid(UUID.randomUUID());

            for (Product product : products) {
                product.getRequiredDocuments().add(document);
            }

            document = requiredDocumentsRepository.save(document);
            productRepository.saveAll(products);

            responseList.add(mapToResponseDto(document));
        }

        return responseList;
    }

    @Override
    public ProductRequiredDocumentsResponseDto getRequiredDocumentById(Long id, Long userId) {
        // Validate user
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found or is deleted"));

        // Find document
        ProductRequiredDocuments document = requiredDocumentsRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Required document with ID " + id + " not found"));

        // Check if user has ADMIN role or is the creator/updater
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        if (!isAdmin && !document.getCreatedBy().equals(userId) && !document.getUpdatedBy().equals(userId)) {
            throw new ValidationException("User is not authorized to access this document");
        }

        return mapToResponseDto(document);
    }

    @Override
    public List<ProductRequiredDocumentsResponseDto> getAllRequiredDocuments(Long userId, int page, int size, String name,
                                                                             String type, String country, String centralName,
                                                                             String stateName) {
        // Validate user
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found or is deleted"));

        // Check if user has ADMIN role
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        if (!isAdmin) {
            throw new ValidationException("Only users with ADMIN role can fetch all required documents");
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<ProductRequiredDocuments> documentPage;

        // Fetch documents based on filters
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

        String name = requestDto.getName().trim();
        String country = Optional.ofNullable(requestDto.getCountry()).orElse("");
        String centralName = Optional.ofNullable(requestDto.getCentralName()).orElse("");
        String stateName = Optional.ofNullable(requestDto.getStateName()).orElse("");

        // Check for duplicate document name and region combination excluding current ID
        if (requiredDocumentsRepository.existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalseExcludingId(id, name, country, centralName, stateName)) {
            throw new ValidationException("Required document with name " + name + " already exists for this region");
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
        document.setUpdatedBy(requestDto.getUpdatedBy());
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

    @Override
    public List<ProductRequiredDocumentsResponseDto> getRequiredDocumentsByProjectAndProduct(Long projectId, Long productId, Long userId) {
        // Validate user
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found or is deleted"));

        // Validate project
        Project project = projectRepository.findByIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with ID " + projectId + " not found or is deleted"));

        // Validate product
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found or is deleted"));

        // Verify product belongs to the project
        if (!project.getProduct().getId().equals(productId)) {
            throw new ValidationException("Product with ID " + productId + " is not associated with project ID " + projectId);
        }

        // Check if user has ADMIN role or is the creator/updater of the project
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        if (!isAdmin && !project.getCreatedBy().equals(userId) && !project.getUpdatedBy().equals(userId)) {
            throw new ValidationException("User is not authorized to access this project's documents");
        }

        // Fetch required documents for the product
        List<ProductRequiredDocuments> documents = product.getRequiredDocuments()
                .stream()
                .filter(doc -> !doc.isDeleted())
                .collect(Collectors.toList());

        // Map to DTOs and include uploads
        return documents.stream()
                .map(doc -> {
                    ProductRequiredDocumentsResponseDto dto = mapToResponseDto(doc);
                    List<ProjectDocumentUpload> uploads = projectDocumentUploadRepository
                            .findByProjectIdAndRequiredDocumentUuidAndIsDeletedFalse(projectId, doc.getUuid());
                    dto.setUploads(uploads.stream()
                            .map(this::mapToDocumentResponseDto)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
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
        if (requestDto.getCountry() == null && requestDto.getCentralName() == null && requestDto.getStateName() == null) {
            throw new ValidationException("At least one of country, centralName, or stateName must be provided for regional documents");
        }
    }

    private void mapRequestDtoToEntity(ProductRequiredDocuments document, ProductRequiredDocumentsRequestDto requestDto) {
        document.setName(requestDto.getName().trim());
        document.setDescription(requestDto.getDescription());
        document.setType(requestDto.getType().trim());
        document.setCountry(Optional.ofNullable(requestDto.getCountry()).orElse(""));
        document.setCentralName(Optional.ofNullable(requestDto.getCentralName()).orElse(""));
        document.setStateName(Optional.ofNullable(requestDto.getStateName()).orElse(""));
    }

    private ProductRequiredDocumentsResponseDto mapToResponseDto(ProductRequiredDocuments document) {
        ProductRequiredDocumentsResponseDto dto = new ProductRequiredDocumentsResponseDto();
        dto.setId(document.getId());
        dto.setUuid(document.getUuid());
        dto.setName(document.getName());
        dto.setDescription(document.getDescription());
        dto.setType(document.getType());
        dto.setCountry(document.getCountry().isEmpty() ? null : document.getCountry());
        dto.setCentralName(document.getCentralName().isEmpty() ? null : document.getCentralName());
        dto.setStateName(document.getStateName().isEmpty() ? null : document.getStateName());
        dto.setCreatedBy(document.getCreatedBy());
        dto.setUpdatedBy(document.getUpdatedBy());
        dto.setCreatedDate(document.getCreatedDate());
        dto.setUpdatedDate(document.getUpdatedDate());

        List<Long> productIds = document.getProducts()
                .stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        dto.setProductIds(productIds);

        return dto;
    }

    // Duplicated mapper for DocumentResponseDto (to avoid dependency; alternatively, inject ProjectDocumentUploadService)
    private DocumentResponseDto mapToDocumentResponseDto(ProjectDocumentUpload documentUpload) {
        DocumentResponseDto dto = new DocumentResponseDto();
        dto.setId(documentUpload.getId());
        dto.setFileUrl(documentUpload.getFileUrl());
        dto.setFileName(documentUpload.getFileName());
        dto.setOldFileUrl(documentUpload.getOldFileUrl());
        dto.setOldFileName(documentUpload.getOldFileName());
        dto.setStatus(documentUpload.getStatus());
        dto.setRemarks(documentUpload.getRemarks());
        dto.setUploadTime(documentUpload.getUploadTime());
        dto.setRequiredDocumentId(documentUpload.getRequiredDocument().getUuid());
        dto.setMilestoneAssignmentId(documentUpload.getMilestoneAssignment().getId());
        dto.setProjectId(documentUpload.getProject().getId());
        dto.setUploadedById(documentUpload.getUploadedBy().getId());
        dto.setCreatedDate(documentUpload.getCreatedDate());
        dto.setUpdatedDate(documentUpload.getUpdatedDate());
        return dto;
    }
}