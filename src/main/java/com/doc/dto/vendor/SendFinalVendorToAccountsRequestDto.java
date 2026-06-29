package com.doc.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendFinalVendorToAccountsRequestDto {

    @NotBlank(message = "Final vendor attachment is required")
    private String finalVendorAttachmentUrl;

    private String finalVendorRemarks;

    @NotNull(message = "User ID is required")
    private Long userId;
}
