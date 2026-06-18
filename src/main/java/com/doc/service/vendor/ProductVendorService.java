package com.doc.service.vendor;

import com.doc.dto.vendor.ProductVendorCreateRequestDto;
import com.doc.dto.vendor.ProductVendorResponseDto;
import com.doc.dto.vendor.ProductVendorUpdateRequestDto;
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



    void removeVendorFromProduct(Long mappingId, Long userId);

    ProductVendorResponseDto updateProductVendorMapping(Long mappingId, Long userId, ProductVendorUpdateRequestDto dto);
}