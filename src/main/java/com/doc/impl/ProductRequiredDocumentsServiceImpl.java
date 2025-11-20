package com.doc.impl;

import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsRequestDto;
import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsResponseDto;
import com.doc.dto.project.DocumentResponseDto;
import com.doc.entity.product.Product;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.project.Project;
import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProductRepository;
import com.doc.repository.documentRepo.ProductRequiredDocumentsRepository;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.service.ProductRequiredDocumentsService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ProjectDocumentUploadRepository projectDocumentUploadRepository;

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
                throw new ValidationException("Required document with name " + name + " already exists for this region", "DUPLICATE_DOCUMENT_NAME");
            }

            User createdBy = userRepository.findActiveUserById(requestDto.getCreatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getCreatedBy() + " not found", "USER_NOT_FOUND"));
            User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getUpdatedBy() + " not found", "USER_NOT_FOUND"));

            List<Product> products = new ArrayList<>();
            if (requestDto.getProductIds() != null && !requestDto.getProductIds().isEmpty()) {
                products = productRepository.findAllById(requestDto.getProductIds())
                        .stream()
                        .filter(p -> !p.isDeleted())
                        .collect(Collectors.toList());
                if (products.size() != requestDto.getProductIds().size()) {
                    throw new ResourceNotFoundException("One or more products not found", "PRODUCT_NOT_FOUND");
                }
            }

            ProductRequiredDocuments document = new ProductRequiredDocuments();


            mapRequestDtoToEntity(document, requestDto);
            document.setCreatedBy(requestDto.getCreatedBy());
            document.setUpdatedBy(requestDto.getUpdatedBy());
            document.setCreatedDate(new Date());
            document.setUpdatedDate(new Date());
            document.setDeleted(false);
            document.setActive(true);  // Added
            document.setProducts(products);

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
    public ProductRequiredDocumentsResponseDto updateRequiredDocument(Long id, ProductRequiredDocumentsRequestDto requestDto) {
        validateRequestDto(requestDto);

        ProductRequiredDocuments document = requiredDocumentsRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Required document with ID " + id + " not found", "DOCUMENT_NOT_FOUND"));

        String name = requestDto.getName().trim();
        String country = Optional.ofNullable(requestDto.getCountry()).orElse("");
        String centralName = Optional.ofNullable(requestDto.getCentralName()).orElse("");
        String stateName = Optional.ofNullable(requestDto.getStateName()).orElse("");

        if (requiredDocumentsRepository.existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalseExcludingId(id, name, country, centralName, stateName)) {
            throw new ValidationException("Required document with name " + name + " already exists for this region", "DUPLICATE_DOCUMENT_NAME");
        }

        User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getUpdatedBy() + " not found", "USER_NOT_FOUND"));

        List<Product> products = new ArrayList<>();
        if (requestDto.getProductIds() != null && !requestDto.getProductIds().isEmpty()) {
            products = productRepository.findAllById(requestDto.getProductIds())
                    .stream()
                    .filter(p -> !p.isDeleted())
                    .collect(Collectors.toList());
            if (products.size() != requestDto.getProductIds().size()) {
                throw new ResourceNotFoundException("One or more products not found", "PRODUCT_NOT_FOUND");
            }
        }

        for (Product product : document.getProducts()) {
            product.getRequiredDocuments().remove(document);
        }
        productRepository.saveAll(document.getProducts());

        mapRequestDtoToEntity(document, requestDto);
        document.setUpdatedBy(requestDto.getUpdatedBy());
        document.setUpdatedDate(new Date());
        document.setProducts(products);

        for (Product product : products) {
            product.getRequiredDocuments().add(document);
        }

        document = requiredDocumentsRepository.save(document);
        productRepository.saveAll(products);

        return mapToResponseDto(document);
    }

    @Override
    public void deleteRequiredDocument(Long id) {
        ProductRequiredDocuments document = requiredDocumentsRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Required document with ID " + id + " not found", "DOCUMENT_NOT_FOUND"));

        for (Product product : document.getProducts()) {
            product.getRequiredDocuments().remove(document);
        }
        productRepository.saveAll(document.getProducts());

        document.setDeleted(true);
        document.setUpdatedDate(new Date());
        requiredDocumentsRepository.save(document);
    }

    @Override
    public List<ProductRequiredDocumentsResponseDto> getRequiredDocumentsByProjectAndProduct(Long projectId, Long productId, Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found or is deleted", "USER_NOT_FOUND"));

        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with ID " + projectId + " not found or is deleted", "PROJECT_NOT_FOUND"));

        Product product = productRepository.findActiveUserById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found or is deleted", "PRODUCT_NOT_FOUND"));

        if (!project.getProduct().getId().equals(productId)) {
            throw new ValidationException("Product with ID " + productId + " is not associated with project ID " + projectId, "INVALID_PRODUCT_PROJECT_ASSOCIATION");
        }

        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        if (!isAdmin && !project.getCreatedBy().equals(userId) && !project.getUpdatedBy().equals(userId)) {
            throw new ValidationException("User is not authorized to access this project's documents", "UNAUTHORIZED_ACCESS");
        }

        List<ProductRequiredDocuments> documents = product.getRequiredDocuments()
                .stream()
                .filter(doc -> !doc.isDeleted())
                .collect(Collectors.toList());

        return documents.stream()
                .map(doc -> {
                    ProductRequiredDocumentsResponseDto dto = mapToResponseDto(doc);
                    List<ProjectDocumentUpload> uploads = projectDocumentUploadRepository
                            .findByProjectIdAndRequiredDocumentIdAndIsDeletedFalse(projectId, doc.getId());
                    dto.setUploads(uploads.stream()
                            .map(this::mapToDocumentResponseDto)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }


    @Override
    public List<ProductRequiredDocumentsResponseDto> getRequiredDocumentsByProduct(Long productId, Long projectId,
                                                                                   String stateName, String centralName) {
        Product product = productRepository.findActiveUserById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found or is deleted", "PRODUCT_NOT_FOUND"));

        Project project = projectRepository.findActiveUserById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with ID " + projectId + " not found or is deleted", "PROJECT_NOT_FOUND"));

        if (!project.getProduct().getId().equals(productId)) {
            throw new ValidationException("Product with ID " + productId + " is not associated with project ID " + projectId, "INVALID_PRODUCT_PROJECT_ASSOCIATION");
        }


        // before i made
        List<ProductRequiredDocuments> documents = product.getRequiredDocuments()
                .stream()
                .filter(doc -> !doc.isDeleted())
                .filter(doc -> {
                    if (stateName != null && !stateName.isEmpty()) {
                        return doc.getStateName().equalsIgnoreCase(stateName);
                    } else if (centralName != null && !centralName.isEmpty()) {
                        return doc.getCentralName().equalsIgnoreCase(centralName) && doc.getStateName().isEmpty();
                    } else {
                        return doc.getStateName().isEmpty() && doc.getCentralName().isEmpty() || // International or central with no state
                                !doc.getCentralName().isEmpty(); // Central-level documents
                    }
                })
                .collect(Collectors.toList());


        return documents.stream()
                .map(doc -> {
                    ProductRequiredDocumentsResponseDto dto = mapToResponseDto(doc);
                    List<ProjectDocumentUpload> uploads = projectDocumentUploadRepository
                            .findByProjectIdAndRequiredDocumentIdAndIsDeletedFalse(projectId, doc.getId());
                    dto.setUploads(uploads.stream()
                            .map(this::mapToDocumentResponseDto)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void validateRequestDto(ProductRequiredDocumentsRequestDto requestDto) {
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new ValidationException("Document name cannot be empty", "INVALID_DOCUMENT_NAME");
        }
        if (requestDto.getType() == null || requestDto.getType().trim().isEmpty()) {
            throw new ValidationException("Document type cannot be empty", "INVALID_DOCUMENT_TYPE");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null", "INVALID_CREATED_BY");
        }
        if (requestDto.getUpdatedBy() == null) {
            throw new ValidationException("Updated by user ID cannot be null", "INVALID_UPDATED_BY");
        }
        if (requestDto.getCountry() == null && requestDto.getCentralName() == null && requestDto.getStateName() == null) {
            throw new ValidationException("At least one of country, centralName, or stateName must be provided for regional documents", "INVALID_REGIONAL_FIELDS");
        }
    }

    private void mapRequestDtoToEntity(ProductRequiredDocuments document, ProductRequiredDocumentsRequestDto requestDto) {
        document.setName(requestDto.getName().trim());
        document.setDescription(requestDto.getDescription());
        document.setType(requestDto.getType().trim());
        document.setCountry(Optional.ofNullable(requestDto.getCountry()).orElse(""));
        document.setCentralName(Optional.ofNullable(requestDto.getCentralName()).orElse(""));
        document.setStateName(Optional.ofNullable(requestDto.getStateName()).orElse(""));
        document.setExpiryType(requestDto.getExpiryType());
        document.setMandatory(requestDto.getIsMandatory() != null ? requestDto.getIsMandatory() : true);
        document.setMaxValidityYears(requestDto.getMaxValidityYears());
        document.setMinFileSizeKb(requestDto.getMinFileSizeKb());
        document.setAllowedFormats(
                requestDto.getAllowedFormats() != null ? requestDto.getAllowedFormats() : "pdf,jpg,png"
        );
    }

    private ProductRequiredDocumentsResponseDto mapToResponseDto(ProductRequiredDocuments document) {
        ProductRequiredDocumentsResponseDto dto = new ProductRequiredDocumentsResponseDto();
        dto.setId(document.getId());
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
        dto.setRequiredDocumentId(documentUpload.getRequiredDocument().getId());
        dto.setMilestoneAssignmentId(documentUpload.getMilestoneAssignment().getId());
        dto.setProjectId(documentUpload.getProject().getId());
        dto.setUploadedById(documentUpload.getUploadedBy().getId());
        dto.setCreatedDate(documentUpload.getCreatedDate());
        dto.setUpdatedDate(documentUpload.getUpdatedDate());
        return dto;
    }

    @Override
    public List<ProductRequiredDocumentsResponseDto> getRequiredDocumentsForAdmin(Long productId, Long userId, String stateName, String centralName) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found or is deleted", "USER_NOT_FOUND"));

        boolean isAuthorized = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN") || role.getName().equals("OPERATION_HEAD"));
        if (!isAuthorized) {
            throw new ValidationException("User is not authorized to access this information", "UNAUTHORIZED_ACCESS");
        }

        Product product = productRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found or is deleted", "PRODUCT_NOT_FOUND"));

        List<ProductRequiredDocuments> documents = product.getRequiredDocuments()
                .stream()
                .filter(doc -> !doc.isDeleted() && doc.isActive())
                .filter(doc -> {
                    if (stateName != null && !stateName.isEmpty()) {
                        return doc.getStateName().equalsIgnoreCase(stateName);
                    } else if (centralName != null && !centralName.isEmpty()) {
                        return doc.getCentralName().equalsIgnoreCase(centralName) && doc.getStateName().isEmpty();
                    } else {
                        return true;
                    }
                })
                .collect(Collectors.toList());

        return documents.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ProductRequiredDocumentsServiceImpl.java
    @Override
    public List<Map<String, Object>> getActiveRequiredDocumentIdsAndNames(Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));

        boolean hasAccess = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()) || "OPERATION_HEAD".equals(role.getName()));

        if (!hasAccess) {
            throw new ValidationException("Access denied. Only ADMIN or OPERATION_HEAD can view this data", "UNAUTHORIZED_ACCESS");
        }

        return requiredDocumentsRepository.findActiveDocumentIdAndName();
    }


}