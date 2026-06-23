package com.doc.service.vendor;

import com.doc.dto.vendor.RFQCreateRequestDto;
import com.doc.dto.vendor.RFQResponseDto;
import com.doc.dto.vendor.RFQSendMailRequestDto;
import com.doc.dto.vendor.RFQUpdateRequestDto;
import com.doc.dto.vendor.RFQVendorResponseDto;
import com.doc.entity.vendor.RFQStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface VendorRFQService {

    Page<RFQResponseDto> getAllRFQs(
            Long productId,
            RFQStatus status,
            Long userId,
            int page,
            int size
    );

    RFQResponseDto createRFQ(Long userId, RFQCreateRequestDto requestDto);

    RFQResponseDto updateRFQ(
            Long rfqId,
            Long userId,
            RFQUpdateRequestDto requestDto
    );

    RFQResponseDto sendRFQToVendors(
            Long rfqId,
            Long userId,
            RFQSendMailRequestDto requestDto
    );

    List<RFQVendorResponseDto> getVendorsByRfqId(Long rfqId);

    RFQResponseDto getRFQById(Long rfqId);

    RFQVendorResponseDto getVendorByRfqIdAndVendorId(Long rfqId, Long vendorId);
}