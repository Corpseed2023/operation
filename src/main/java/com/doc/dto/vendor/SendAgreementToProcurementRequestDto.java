package com.doc.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SendAgreementToProcurementRequestDto {

    @NotBlank(message = "Agreement PDF is required")
    private String agreementFileUrl;

    @NotBlank(message = "Expiry Date is required")
    private Date expiryDate;

    @NotBlank(message = "Validity Days is required")
    private Long validityDays;

    private String remarks;
}