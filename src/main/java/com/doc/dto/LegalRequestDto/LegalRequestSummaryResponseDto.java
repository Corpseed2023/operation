package com.doc.dto.LegalRequestDto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LegalRequestSummaryResponseDto {
    private long totalPending; // status == INITIATED
    private List<LegalRequestStatusCountDto> statusCounts;
}