package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorGSTRegistrationType;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class VendorAccountsSubmissionResponseDto {

    private Long id;

    private Long vendorFinalizationId;

    private Long vendorId;
    private String vendorName;
    private String vendorEmail;
    private String vendorMobile;

    private Long rfqId;
    private String rfqNumber;

    private Long quotationId;
    private String quotationNumber;

    private String name;
    private String number;
    private String email;
    private String aadhar;

    private String authorizedSignatoryName;
    private String authorizedSignatoryNumber;
    private String authorizedSignatoryEmail;
    private String authorizedSignatoryAadhar;

    private String accountHolderName;
    private String accountNumber;
    private String ifsc;
    private String swiftCode;
    private String branchAddress;

    private String gstDetailsUrl;
    private String vendorSetupFormUrl;
    private String cancelChequeUrl;
    private String itrLastFinancialYearUrl;
    private String panDetailsUrl;
    private String partnershipOrCoiUrl;
    private String deedOrMsmeUrl;
    private String balanceSheetUrl;

    private String remarks;

    private String status;

    private Long sentToAccountsBy;
    private Date sentToAccountsDate;

    private Long accountsVerifiedBy;
    private Date accountsVerifiedDate;
    private String accountsRemark;

    private VendorGSTRegistrationType gstRegistrationType;
    private String gstNumber;

    private Long createdBy;
    private Long updatedBy;
    private Date createdDate;
    private Date updatedDate;
    private Boolean deleted;
}