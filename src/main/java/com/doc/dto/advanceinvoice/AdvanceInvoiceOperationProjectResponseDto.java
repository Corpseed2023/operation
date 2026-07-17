package com.doc.dto.advanceinvoice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdvanceInvoiceOperationProjectResponseDto {

    private Long projectId;
    private String projectNo;
    private String projectName;

    private Long estimateId;
    private String estimateNumber;

    private Long sourceInvoiceId;
    private String sourceInvoiceNumber;

    private String status;
    private boolean created;
}