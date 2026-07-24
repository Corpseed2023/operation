package com.doc.dto.vendor;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountVendorSyncRequestDto {

    private Long operationVendorId;

    private String vendorName;

    private String email;

    private String mobile;

    private String pan;

    private String gstNumber;

    /**
     * Send enum as String to avoid coupling both services.
     */
    private String gstRegistrationType;

    private String accountHolderName;

    private String bankAccountNumber;

    private String bankName;

    private String ifscCode;

    private Boolean active;

    private LocalDateTime operationUpdatedAt;
}
