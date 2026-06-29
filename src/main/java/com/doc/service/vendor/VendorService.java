package com.doc.service.vendor;

import com.doc.dto.vendor.VendorRequestDto;
import com.doc.dto.vendor.VendorResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface VendorService {

    VendorResponseDto getVendorById(Long id);

    VendorResponseDto getVendorDetailsById(Long id);


    VendorResponseDto updateVendor(Long id, Long userId, VendorRequestDto dto);

    void deleteVendor(Long id);

    Page<VendorResponseDto> getAllVendors(Long userId, int page, int size, String keyword);


    VendorResponseDto createVendor(Long userId, VendorRequestDto dto);
}
