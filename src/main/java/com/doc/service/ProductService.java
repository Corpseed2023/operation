package com.doc.service;


import com.doc.dto.product.ProductRequestDto;
import com.doc.dto.product.ProductResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface ProductService {

    ProductResponseDto createProduct(ProductRequestDto requestDto);

    ProductResponseDto getProductById(Long id);

    List<ProductResponseDto> getAllProducts(int page, int size, String productName, Boolean isActive, LocalDate startDate, LocalDate endDate);

    void deleteProduct(Long id);
}
