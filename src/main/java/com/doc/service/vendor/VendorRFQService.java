package com.doc.service.vendor;

import com.doc.dto.vendor.RFQCreateRequestDto;
import com.doc.dto.vendor.RFQResponseDto;
import com.doc.dto.vendor.RFQSendMailRequestDto;
import com.doc.dto.vendor.RFQUpdateRequestDto;
import com.doc.entity.vendor.RFQStatus;
import org.springframework.data.domain.Page;

public interface VendorRFQService {
    Page<RFQResponseDto> getAllRFQs(
            Long productId,
            RFQStatus status,
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
}
