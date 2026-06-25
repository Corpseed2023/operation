package com.doc.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendAgreementToVendorRequestDto {

    @NotBlank(message = "Attachment URL is required")
    private String attachmentUrl;

    private String remarks;

    private String subject;

    private String message;
}