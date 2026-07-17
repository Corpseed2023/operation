package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorGSTRegistrationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorAccountsSubmissionRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String number;

    @NotBlank
    private String email;

    @NotBlank
    private String aadhar;

    @NotBlank
    private String authorizedSignatoryName;

    @NotBlank
    private String authorizedSignatoryNumber;

    @NotBlank
    private String authorizedSignatoryEmail;

    @NotBlank
    private String authorizedSignatoryAadhar;

    @NotBlank
    private String accountHolderName;

    @NotBlank
    private String accountNumber;

    @NotBlank
    private String ifsc;

    private String swiftCode;

    @NotBlank
    private String branchAddress;

    @NotBlank
    private String gstDetailsUrl;

    @NotBlank
    private String vendorSetupFormUrl;

    @NotBlank
    private String cancelChequeUrl;

    private String itrLastFinancialYearUrl;

    @NotBlank
    private String panDetailsUrl;

    private String partnershipOrCoiUrl;

    private String deedOrMsmeUrl;

    private String balanceSheetUrl;

    @NotNull(message = "GST registration type is required")
    private VendorGSTRegistrationType gstRegistrationType;

    private String gstNumber;

    private String remarks;

    @NotNull
    private Long sentToAccountsBy;
}