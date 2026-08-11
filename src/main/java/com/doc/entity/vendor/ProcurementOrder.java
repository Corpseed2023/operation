package com.doc.entity.vendor;

import com.doc.entity.client.PaymentType;
import com.doc.entity.project.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "procurement_orders")
@Getter
@Setter
@NoArgsConstructor
public class ProcurementOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // PROCUREMENT
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "procurement_assignment_id",
            nullable = false
    )
    private ProcurementMilestoneAssignment procurementAssignment;

    // =========================================================
    // PROJECT
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // =========================================================
    // VENDOR
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "vendor_id",
            nullable = false
    )
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_contact_id")
    private VendorContact vendorContact;

    // =========================================================
    // PO DETAILS
    // =========================================================

    @Column(length = 50, unique = true)
    @Comment("System generated PO Number - e.g. PO-2026-00123")
    private String poNumber;

    @Column(length = 50)
    private String poReferenceNumber;

    // =========================================================
    // GST DETAILS
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private VendorGSTRegistrationType vendorGSTRegistrationType;

    /**
     * Buyer / Place of Supply GST state code.
     *
     * Example:
     * Uttar Pradesh = 09
     * Delhi = 07
     */
    @Column(length = 2)
    private String placeOfSupplyStateCode;

    // =========================================================
    // AMOUNT & TAX
    // =========================================================

    @Column(precision = 19, scale = 2)
    private BigDecimal finalAmount;

    /**
     * Total GST percentage.
     * Example: 18.00
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal gstRate;

    @Column(precision = 19, scale = 2)
    private BigDecimal cgstAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal sgstAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal igstAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal tdsPercentage;

    @Column(precision = 19, scale = 2)
    private BigDecimal tdsAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalTaxAmount;

    /**
     * Current business formula:
     *
     * Grand Total =
     * Base Amount + GST - TDS
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal grandTotal;

    // =========================================================
    // COMMERCIAL
    // =========================================================

    @Column(length = 2000)
    private String scopeOfWork;

    @Column(length = 2000)
    private String termsAndConditions;

    @Column(length = 2000)
    private Integer paymentTerms;

    @Column(length = 1000)
    private String remarks;

    // =========================================================
    // ATTACHMENTS
    // =========================================================

    @ElementCollection
    @CollectionTable(
            name = "procurement_order_attachments",
            joinColumns = @JoinColumn(
                    name = "procurement_order_id"
            )
    )
    @Column(name = "file_url")
    private List<String> attachmentUrls = new ArrayList<>();

    // =========================================================
    // STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcurementOrderStatus status =
            ProcurementOrderStatus.DRAFT;

    // =========================================================
    // DATES
    // =========================================================

    @Temporal(TemporalType.TIMESTAMP)
    private Date poCreatedDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date poSubmittedForApprovalDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date poApprovedDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date poReleasedDate;

    // =========================================================
    // PAYMENT TYPE
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_id")
    private PaymentType paymentType;

    // =========================================================
    // AUDIT
    // =========================================================

    private Long createdBy;

    private Long updatedBy;

    private Long approvedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private boolean isDeleted = false;
}