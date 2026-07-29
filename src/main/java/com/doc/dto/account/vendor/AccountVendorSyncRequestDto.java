package com.doc.dto.account.vendor;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVendorSyncRequestDto {

    private Long operationVendorId;

    private Long vendorAccountsSubmissionId;

    private Long vendorFinalizationId;

    private String vendorName;

    private String email;

    private String mobile;

    private String pan;

    private String gstNumber;

    private String gstRegistrationType;

    private String accountHolderName;

    private String bankAccountNumber;

    private String ifscCode;

    private String bankName;

    private String branchAddress;

    private String fullAddress;

    private String city;

    private String state;

    private String country;

    private Boolean active;

    private Long approvedByOperationUserId;

    private LocalDateTime approvedAt;

    private LocalDateTime operationUpdatedAt;

    private VendorVoucherRequestDto voucherDetails;
}