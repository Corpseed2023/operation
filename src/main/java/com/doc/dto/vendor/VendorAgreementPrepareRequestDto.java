package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorAgreementPrepareRequestDto {
    private String agreementFileUrl;
    private Long preparedBy;
    private String remarks;
}