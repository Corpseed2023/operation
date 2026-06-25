package com.doc.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendAgreementToProcurementRequestDto {

    @NotBlank(message = "Agreement PDF is required")
    private String agreementFileUrl;

    private String remarks;
}