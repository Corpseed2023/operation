package com.doc.entity.vendor;

import com.doc.entity.project.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "procurement_payment_requests")
@Getter
@Setter
@NoArgsConstructor
public class ProcurementPaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procurement_order_id", nullable = false)
    private ProcurementOrder procurementOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    // Payment Details
    private BigDecimal invoiceAmount;
    private BigDecimal payableAmount;           // After deductions if any

    @Column(length = 100)
    private String invoiceNumber;

    private Date invoiceDate;
    private Date submissionDate;                // When vendor submitted proof

    @Column(length = 2000)
    private String completionRemarks;

    // Proof/Documents submitted by vendor
    @ElementCollection
    @CollectionTable(name = "procurement_payment_attachments",
            joinColumns = @JoinColumn(name = "payment_request_id"))
    @Column(name = "file_url")
    private List<String> proofAttachmentUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRequestStatus status = PaymentRequestStatus.PENDING;

    private Date approvedDate;
    private Date paymentReleasedDate;

    private Long createdBy;
    private Long approvedBy;
    private Long paymentReleasedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private boolean isDeleted = false;
    @Column(name = "tds_active")
    private String tdsActive;

    @Column(name = "tds_percentage")
    private BigDecimal tdsPercentage;

    @Column(name = "gst_active")
    private String gstActive;

    @Column(name = "gst_state_code")
    private String gstStateCode;

    @Column(name = "gst_percentage")
    private BigDecimal gstPercentage;

    @Column(name = "cgst_amount")
    private BigDecimal cgstAmount;

    @Column(name = "sgst_amount")
    private BigDecimal sgstAmount;

    @Column(name = "igst_amount")
    private BigDecimal igstAmount;

    @Column(name = "total_gst_amount")
    private BigDecimal totalGstAmount;

}