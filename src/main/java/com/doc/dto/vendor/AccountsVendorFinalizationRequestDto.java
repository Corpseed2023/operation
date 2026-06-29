package com.doc.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountsVendorFinalizationRequestDto {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Accounts remark is required")
    private String accountsRemark;
}