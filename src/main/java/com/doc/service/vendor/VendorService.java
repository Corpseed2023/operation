package com.doc.service.vendor;

import com.doc.dto.vendor.*;
import com.doc.entity.vendor.VendorRestrictionRequestStatus;
import com.doc.entity.vendor.VendorStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface VendorService {

    VendorResponseDto getVendorById(Long id);

    VendorResponseDto getVendorDetailsById(Long id);


    VendorResponseDto updateVendor(Long id, Long userId, VendorRequestDto dto);

    void deleteVendor(Long id);

    Page<VendorResponseDto> getAllVendors(
            Long userId,
            int page,
            int size,
            String keyword,
            VendorStatus status
    );


    VendorResponseDto createVendor(Long userId, VendorRequestDto dto);

    VendorRestrictionResponseDto restrictVendor(
            Long vendorId,
            Long userId,
            VendorRestrictionRequestDto dto
    );

    VendorRestrictionResponseDto reviewRestrictionByAccounts(
            Long requestId,
            Long userId,
            VendorRestrictionAccountsReviewDto dto
    );

    VendorRestrictionResponseDto reviewRestrictionByAdmin(
            Long requestId,
            Long userId,
            VendorRestrictionAdminReviewDto dto
    );

    Page<VendorRestrictionResponseDto> getAccountsRestrictionRequests(
            Long userId,
            int page,
            int size,
            VendorRestrictionRequestStatus status
    );

    Page<VendorRestrictionResponseDto> getAdminRestrictionRequests(
            Long userId,
            int page,
            int size,
            VendorRestrictionRequestStatus status
    );
}
