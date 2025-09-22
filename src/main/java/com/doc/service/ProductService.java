package com.doc.service;


import com.doc.dto.product.ProductRequestDto;
import com.doc.dto.product.ProductResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface ProductService {

    ProductResponseDto getProductById(Long id);

    List<ProductResponseDto> getAllProducts(int page, int size);

    void deleteProduct(Long id);

    List<ProductResponseDto> createProducts(List<ProductRequestDto> requestDtoList);
}
