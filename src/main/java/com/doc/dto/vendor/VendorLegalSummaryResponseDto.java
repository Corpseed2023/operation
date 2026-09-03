package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class VendorLegalSummaryResponseDto {
    private long totalPending; // status == SERVICE_AGREEMENT_REQUESTED
    private List<VendorLegalStatusCountDto> statusCounts;
}