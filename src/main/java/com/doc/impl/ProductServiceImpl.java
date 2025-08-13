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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<ProductResponseDto> createProducts(List<ProductRequestDto> requestDtoList) {
        List<ProductResponseDto> responseList = new ArrayList<>();

        for (ProductRequestDto requestDto : requestDtoList) {
            validateRequestDto(requestDto);

            if (productRepository.existsByProductNameAndIsDeletedFalse(requestDto.getProductName().trim())) {
                throw new ValidationException("Product with name " + requestDto.getProductName() + " already exists");
            }

            User createdBy = userRepository.findActiveUserById(requestDto.getCreatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getCreatedBy() + " not found"));

            User updatedBy = userRepository.findActiveUserById(requestDto.getUpdatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getUpdatedBy() + " not found"));

            Product product = new Product();
            mapRequestDtoToEntity(product, requestDto);
            product.setCreatedBy(createdBy);
            product.setUpdatedBy(updatedBy);
            product.setCreatedDate(new Date());
            product.setUpdatedDate(new Date());
            product.setDeleted(false);
            product.setActive(requestDto.isActive());

            product = productRepository.save(product);

            responseList.add(mapToResponseDto(product));
        }

        return responseList;
    }


    @Override
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found"));
        return mapToResponseDto(product);
    }

    @Override
    public List<ProductResponseDto> getAllProducts(int page, int size, String productName, Boolean isActive,
                                                   LocalDate startDate, LocalDate endDate) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Product> productPage;

        if (productName != null || isActive != null || startDate != null || endDate != null) {
            productPage = productRepository.findByFilters(
                    productName != null ? productName.trim() : null,
                    isActive,
                    startDate,
                    endDate,
                    pageable
            );
        } else {
            productPage = productRepository.findByIsDeletedFalse(pageable);
        }

        return productPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found"));

        product.setDeleted(true);
        product.setUpdatedDate(new Date());
        productRepository.save(product);
    }

    private void validateRequestDto(ProductRequestDto requestDto) {
        if (requestDto.getProductName() == null || requestDto.getProductName().trim().isEmpty()) {
            throw new ValidationException("Product name cannot be empty");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null");
        }
        if (requestDto.getUpdatedBy() == null) {
            throw new ValidationException("Updated by user ID cannot be null");
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