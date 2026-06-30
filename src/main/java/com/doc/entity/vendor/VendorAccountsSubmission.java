package com.doc.entity.vendor;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(
        name = "vendor_accounts_submissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vendor_finalization_accounts_submission",
                        columnNames = "vendor_finalization_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorAccountsSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_finalization_id", nullable = false)
    private VendorFinalization vendorFinalization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id")
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private VendorQuotation quotation;

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

    @Column(length = 1000)
    private String branchAddress;

    @Column(length = 500, nullable = false)
    private String gstDetailsUrl;

    @Column(length = 500, nullable = false)
    private String vendorSetupFormUrl;

    @Column(length = 500, nullable = false)
    private String cancelChequeUrl;

    @Column(length = 500)
    private String itrLastFinancialYearUrl;

    @Column(length = 500, nullable = false)
    private String panDetailsUrl;

    @Column(length = 500 )
    private String partnershipOrCoiUrl;

    @Column(length = 500)
    private String deedOrMsmeUrl;

    @Column(length = 500)
    private String balanceSheetUrl;

    @Column(length = 1000)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VendorAccountsSubmissionStatus status =
            VendorAccountsSubmissionStatus.PENDING;

    private Long sentToAccountsBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date sentToAccountsDate;

    private Long accountsVerifiedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date accountsVerifiedDate;

    @Column(length = 1000)
    private String accountsRemark;

    private Long createdBy;
    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        createdDate = new Date();
        updatedDate = new Date();

        if (sentToAccountsDate == null) {
            sentToAccountsDate = new Date();
        }

        if (status == null) {
            status = VendorAccountsSubmissionStatus.PENDING;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedDate = new Date();
    }
}