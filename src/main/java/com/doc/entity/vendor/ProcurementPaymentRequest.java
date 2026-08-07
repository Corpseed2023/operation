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

/**
 * One record represents one vendor invoice and its full payment settlement.
 *
 * Monetary contract:
 * amount + totalGstAmount = invoiceAmount
 * invoiceAmount - tdsAmount = payableAmount
 * bankPaymentAmount = payableAmount when PAYMENT_RELEASED
 */
@Entity
@Table(
        name = "procurement_payment_requests",
        indexes = {
                @Index(
                        name = "idx_proc_payment_order_status",
                        columnList = "procurement_order_id,status,is_deleted"
                ),
                @Index(
                        name = "idx_proc_payment_vendor",
                        columnList = "vendor_id,is_deleted"
                )
        }
)
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

    /** GST-inclusive vendor invoice total. */
    @Column(
            name = "invoice_amount",
            precision = 19,
            scale = 2,
            nullable = false
    )
    private BigDecimal invoiceAmount;

    /** Net amount payable through Bank/Cash after TDS. */
    @Column(
            name = "payable_amount",
            precision = 19,
            scale = 2,
            nullable = false
    )
    private BigDecimal payableAmount;

    /** Taxable/basic purchase value before GST; never overwrite at release. */
    @Column(
            name = "payment_amount",
            precision = 19,
            scale = 2,
            nullable = false
    )
    private BigDecimal amount;

    /** Actual Bank/Cash amount released. Separate from taxable amount. */
    @Column(
            name = "bank_payment_amount",
            precision = 19,
            scale = 2
    )
    private BigDecimal bankPaymentAmount;

//    @Column(name = "invoice_number", length = 100)
//    private String invoiceNumber;
//
//    @Column(name = "invoice_date")
//    private LocalDate invoiceDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

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
            joinColumns = @JoinColumn(name = "payment_request_id")
    )
    @Column(name = "file_url")
    private List<String> proofAttachmentUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private PaymentRequestStatus status = PaymentRequestStatus.PENDING;

    private Long createdBy;
    private Long approvedBy;
    private Long paymentReleasedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "gst_active", nullable = false)
    private Boolean gstActive = false;

    /** INTRA_STATE or INTER_STATE. */
    @Column(name = "gst_type", length = 30)
    private String gstType;

    @Column(name = "gst_state_code", length = 10)
    private String gstStateCode;

    @Column(name = "gst_percentage", precision = 10, scale = 4)
    private BigDecimal gstPercentage;

    @Column(name = "cgst_amount", precision = 19, scale = 2)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_amount", precision = 19, scale = 2)
    private BigDecimal sgstAmount;

    @Column(name = "igst_amount", precision = 19, scale = 2)
    private BigDecimal igstAmount;

    @Column(name = "total_gst_amount", precision = 19, scale = 2)
    private BigDecimal totalGstAmount;

    @Column(name = "tds_active", nullable = false)
    private Boolean tdsActive = false;

    @Column(name = "tds_percentage", precision = 10, scale = 4)
    private BigDecimal tdsPercentage;

    @Column(name = "tds_amount", precision = 19, scale = 2)
    private BigDecimal tdsAmount;

    @Column(name = "payment_mode", length = 50)
    private String paymentMode;

    @Column(name = "bank_ledger_id")
    private Long bankLedgerId;

    /** Optional legacy vendor ledger metadata; Account Service resolves the vendor ledger. */
    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "ledger_type", length = 50)
    private String ledgerType;

    @Column(name = "transaction_reference", length = 150)
    private String transactionReference;

    @Column(name = "payment_proof", length = 1000)
    private String paymentProof;

    @Column(name = "calculation_version", length = 40)
    private String calculationVersion;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @PrePersist
    void onCreate() {
        Date now = new Date();
        if (createdDate == null) {
            createdDate = now;
        }
        updatedDate = now;
        if (submissionDate == null) {
            submissionDate = now;
        }
        if (status == null) {
            status = PaymentRequestStatus.PENDING;
        }
        if (gstActive == null) {
            gstActive = false;
        }
        if (tdsActive == null) {
            tdsActive = false;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedDate = new Date();
    }
}
