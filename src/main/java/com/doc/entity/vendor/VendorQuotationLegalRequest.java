package com.doc.entity.vendor;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(
        name = "vendor_quotation_legal_requests",
        indexes = {
                @Index(name = "idx_vqlr_quotation", columnList = "vendor_quotation_id"),
                @Index(name = "idx_vqlr_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorQuotationLegalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Vendor quotation for which legal help/review is requested.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_quotation_id", nullable = false)
    private VendorQuotation vendorQuotation;

    @Column(nullable = false, length = 255)
    private String legalRequestTitle;

    @Column(length = 2000)
    private String notes;

    @Column(length = 1000)
    private String statusReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VendorQuotationLegalRequestStatus status =
            VendorQuotationLegalRequestStatus.SERVICE_AGREEMENT_REQUESTED;

    private Long assignedToLegal;

    private Long createdBy;

    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedDate = new Date();
    }

    @Column(length = 500)
    private String agreementFileUrl;

    private Long agreementPreparedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date agreementPreparedDate;

    private Long sentToOperationBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date sentToOperationDate;

    private Long sentToVendorBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date sentToVendorDate;

    private Long decisionBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date decisionDate;

    @Column(length = 1000)
    private String decisionRemarks;
}