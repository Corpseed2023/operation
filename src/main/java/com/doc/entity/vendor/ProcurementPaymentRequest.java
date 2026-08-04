package com.doc.entity.vendor;

import com.doc.entity.project.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @JoinColumn(
            name = "procurement_order_id",
            nullable = false
    )
    private ProcurementOrder procurementOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "vendor_id",
            nullable = false
    )
    private Vendor vendor;

    /*
     * Commercial amounts captured by Operation Service.
     */
    @Column(
            name = "invoice_amount",
            precision = 19,
            scale = 2,
            nullable = false
    )
    private BigDecimal invoiceAmount;

    @Column(
            name = "payable_amount",
            precision = 19,
            scale = 2,
            nullable = false
    )
    private BigDecimal payableAmount;

    /*
     * Basic vendor price before GST.
     *
     * Account Service uses this as the GST/TDS calculation base.
     */
    @Column(
            name = "payment_amount",
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "invoice_number",
            length = 100
    )
    private String invoiceNumber;

    /*
     * Invoice date is a date-only value.
     */
    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    /*
     * Workflow/audit timestamps.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "submission_date")
    private Date submissionDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "approved_date")
    private Date approvedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "payment_released_date")
    private Date paymentReleasedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    private Date updatedDate;

    @Column(length = 2000)
    private String completionRemarks;

    @ElementCollection
    @CollectionTable(
            name = "procurement_payment_attachments",
            joinColumns = @JoinColumn(
                    name = "payment_request_id"
            )
    )
    @Column(name = "file_url")
    private List<String> proofAttachmentUrls =
            new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 40
    )
    private PaymentRequestStatus status =
            PaymentRequestStatus.PENDING;

    private Long createdBy;

    private Long approvedBy;

    private Long paymentReleasedBy;

    @Column(
            name = "is_deleted",
            nullable = false
    )
    private boolean isDeleted = false;

    /*
     * GST inputs.
     */
    @Column(
            name = "gst_active",
            nullable = false
    )
    private Boolean gstActive = false;

    /*
     * Supply type only:
     *
     * INTRA_STATE
     * INTER_STATE
     */
    @Column(
            name = "gst_type",
            length = 30
    )
    private String gstType;

    @Column(
            name = "gst_state_code",
            length = 10
    )
    private String gstStateCode;

    @Column(
            name = "gst_percentage",
            precision = 10,
            scale = 4
    )
    private BigDecimal gstPercentage;

    /*
     * These calculated fields may remain for reporting,
     * but Account Service is the source of truth for calculation.
     */
    @Column(
            name = "cgst_amount",
            precision = 19,
            scale = 2
    )
    private BigDecimal cgstAmount;

    @Column(
            name = "sgst_amount",
            precision = 19,
            scale = 2
    )
    private BigDecimal sgstAmount;

    @Column(
            name = "igst_amount",
            precision = 19,
            scale = 2
    )
    private BigDecimal igstAmount;

    @Column(
            name = "total_gst_amount",
            precision = 19,
            scale = 2
    )
    private BigDecimal totalGstAmount;

    /*
     * TDS inputs.
     */
    @Column(
            name = "tds_active"
    )
    private Boolean tdsActive = false;

    @Column(
            name = "tds_percentage",
            precision = 10,
            scale = 4
    )
    private BigDecimal tdsPercentage;

    @Column(
            name = "tds_amount",
            precision = 19,
            scale = 2
    )
    private BigDecimal tdsAmount;

    /*
     * Payment release metadata.
     */
    @Column(
            name = "payment_mode",
            length = 50
    )
    private String paymentMode;

    @Column(name = "bank_ledger_id")
    private Long bankLedgerId;

    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(
            name = "ledger_type",
            length = 50
    )
    private String ledgerType;

    @Column(
            name = "transaction_reference",
            length = 150
    )
    private String transactionReference;

    @Column(
            name = "payment_proof",
            length = 1000
    )
    private String paymentProof;
}
