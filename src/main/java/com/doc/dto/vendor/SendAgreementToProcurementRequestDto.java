package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendAgreementToProcurementRequestDto {

    private String agreementFileUrl;

    private String remarks;
}