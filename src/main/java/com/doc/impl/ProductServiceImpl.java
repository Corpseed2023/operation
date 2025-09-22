package com.doc.impl;

import com.doc.dto.product.ProductRequestDto;
import com.doc.dto.product.ProductResponseDto;
import com.doc.entity.product.Product;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProductRepository;
import com.doc.repository.UserRepository;
import com.doc.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<ProductResponseDto> createProducts(List<ProductRequestDto> requestDtoList) {
        logger.info("Creating products for request: {}", requestDtoList);
        List<ProductResponseDto> responseList = new ArrayList<>();

        for (ProductRequestDto requestDto : requestDtoList) {
            validateRequestDto(requestDto);

            // Validate that id is provided by client (since no auto-generation now)
            if (requestDto.getProductId() == null) {
                throw new ValidationException("Product ID must be provided", "INVALID_PRODUCT_ID");
            }

            // Check if product with same id already exists (avoid overwrite)
            if (productRepository.existsById(requestDto.getProductId())) {
                throw new ValidationException("Product with ID " + requestDto.getProductId() + " already exists", "DUPLICATE_PRODUCT_ID");
            }

            // Check for duplicate product name as before
            if (productRepository.existsByProductNameAndIsDeletedFalse(requestDto.getProductName().trim())) {
                throw new ValidationException("Product with name " + requestDto.getProductName() + " already exists", "DUPLICATE_PRODUCT_NAME");
            }

            User createdBy = userRepository.findActiveUserById(requestDto.getCreatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getCreatedBy() + " not found", "USER_NOT_FOUND"));

            User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getUpdatedBy() + " not found", "USER_NOT_FOUND"));

            Product product = new Product();

            // Explicitly set id from DTO
            product.setId(requestDto.getProductId());

            mapRequestDtoToEntity(product, requestDto);
            product.setCreatedBy(createdBy);
            product.setUpdatedBy(updatedBy);
            product.setCreatedDate(new Date());
            product.setUpdatedDate(new Date());
            product.setDeleted(false);
            product.setActive(requestDto.isActive());

            product = productRepository.save(product);
            logger.info("Product created successfully with ID: {}", product.getId());

            responseList.add(mapToResponseDto(product));
        }
        return responseList;
    }

    @Override
    public ProductResponseDto getProductById(Long id) {
        logger.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found", "PRODUCT_NOT_FOUND"));
        return mapToResponseDto(product);
    }

    @Override
    public List<ProductResponseDto> getAllProducts(int page, int size) {
        logger.info("Fetching active and non-deleted products, page: {}, size: {}", page, size);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findByIsActiveTrueAndIsDeletedFalse(pageable);

        List<ProductResponseDto> responses = productPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        logger.info("Retrieved {} active and non-deleted products", responses.size());
        return responses;
    }

    @Override
    public void deleteProduct(Long id) {
        logger.info("Deleting product with ID: {}", id);
        Product product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found", "PRODUCT_NOT_FOUND"));

        product.setDeleted(true);
        product.setUpdatedDate(new Date());
        productRepository.save(product);
        logger.info("Product deleted successfully with ID: {}", id);
    }

    private void validateRequestDto(ProductRequestDto requestDto) {
        if (requestDto.getProductName() == null || requestDto.getProductName().trim().isEmpty()) {
            throw new ValidationException("Product name cannot be empty", "INVALID_PRODUCT_NAME");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null", "INVALID_CREATED_BY");
        }
        if (requestDto.getUpdatedBy() == null) {
            throw new ValidationException("Updated by user ID cannot be null", "INVALID_UPDATED_BY");
        }
    }

    private void mapRequestDtoToEntity(Product product, ProductRequestDto requestDto) {
        product.setProductName(requestDto.getProductName().trim());
        product.setDescription(requestDto.getDescription());
        product.setDate(requestDto.getDate());
        product.setActive(requestDto.isActive());
    }

    private ProductResponseDto mapToResponseDto(Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(product.getId());
        dto.setProductName(product.getProductName());
        dto.setDescription(product.getDescription());
        dto.setCreatedBy(product.getCreatedBy() != null ? product.getCreatedBy().getId() : null);
        dto.setUpdatedBy(product.getUpdatedBy() != null ? product.getUpdatedBy().getId() : null);
        dto.setDate(product.getDate());
        dto.setCreatedDate(product.getCreatedDate());
        dto.setUpdatedDate(product.getUpdatedDate());
        dto.setActive(product.isActive());
        return dto;
    }
}