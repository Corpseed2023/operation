package com.doc.service.vendor;

import com.doc.dto.vendor.VendorRequestDto;
import com.doc.dto.vendor.VendorResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface VendorService {

    VendorResponseDto createVendor(VendorRequestDto dto);

    VendorResponseDto updateVendor(Long id, VendorRequestDto dto);

    VendorResponseDto getVendorById(Long id);


    List<VendorResponseDto> getVendorsByProduct(Long productId);

    void deleteVendor(Long id);

    Page<VendorResponseDto> getAllVendors(Long userId, int page, int size, String keyword);


}