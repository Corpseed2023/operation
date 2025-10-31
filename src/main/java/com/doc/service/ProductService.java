package com.doc.service;

import com.doc.dto.product.ProductRequestDto;
import com.doc.dto.product.ProductResponseDto;

import java.util.List;

public interface ProductService {

    ProductResponseDto getProductById(Long id);

    void deleteProduct(Long id);

    List<ProductResponseDto> createProducts(List<ProductRequestDto> requestDtoList);

    List<ProductResponseDto> getAllProducts(Long userId, int page, int size);

}