package com.doc.dto.vendor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorRestrictionAccountsReviewDto {

    @NotNull(message = "Approval status is required")
    private Boolean approved;

    @Size(
            max = 1000,
            message = "Accounts remarks cannot exceed 1000 characters"
    )
    private String remarks;
}