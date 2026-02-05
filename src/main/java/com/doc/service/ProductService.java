package com.doc.service;

import com.doc.dto.product.ProductRequestDto;
import com.doc.dto.product.ProductResponseDto;

import java.util.List;

public interface ProductService {

    ProductResponseDto getProductById(Long id);

    void deleteProduct(Long id);

    ProductResponseDto createProduct(ProductRequestDto requestDto);

    List<ProductResponseDto> getAllProducts(Long userId, int page, int size);

}