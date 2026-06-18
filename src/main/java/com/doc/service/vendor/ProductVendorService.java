package com.doc.service.vendor;

import com.doc.dto.vendor.ProductVendorCreateRequestDto;
import com.doc.dto.vendor.ProductVendorResponseDto;
import org.springframework.data.domain.Page;

public interface ProductVendorService {

    ProductVendorResponseDto createVendorAgainstProduct(
            Long productId,
            Long userId,
            ProductVendorCreateRequestDto dto
    );

    Page<ProductVendorResponseDto> getVendorsByProduct(
            Long productId,
            Long userId,
            int page,
            int size
    );

    ProductVendorResponseDto updateProductVendorMapping(
            Long mappingId,
            Long userId,
            ProductVendorCreateRequestDto dto
    );

    void removeVendorFromProduct(Long mappingId, Long userId);
}