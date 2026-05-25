package com.doc.entity.vendor;

import com.doc.entity.client.PaymentType;

import com.doc.entity.project.ProcurementMilestoneAssignment;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procurement_assignment_id", nullable = false)
    private ProcurementMilestoneAssignment procurementAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_contact_id")
    private VendorContact vendorContact;

    @Column(length = 50, unique = true)
    @Comment("System generated PO Number - e.g. PO-2026-00123")
    private String poNumber;

    @Column(length = 50)
    private String poReferenceNumber;

    // ==================== AMOUNT & TAX BREAKUP ====================

    private BigDecimal estimatedAmount;
    private BigDecimal finalAmount;           // Base amount before tax

    private BigDecimal gstRate;               // e.g. 18.00

    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;

    private BigDecimal totalTaxAmount;
    private BigDecimal grandTotal;            // Final payable amount

    // ==================== COMMERCIAL DETAILS ====================

    @Column(length = 2000)
    private String scopeOfWork;

    @Column(length = 1000)
    private String paymentTerms;

    @Column(length = 2000)
    private String termsAndConditions;

    @Column(length = 1000)
    private String remarks;

    // ==================== ATTACHMENTS ====================

    @ElementCollection
    @CollectionTable(name = "procurement_order_attachments",
            joinColumns = @JoinColumn(name = "procurement_order_id"))
    @Column(name = "file_url")
    private List<String> attachmentUrls = new ArrayList<>();

    // ==================== STATUS & DATES ====================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcurementOrderStatus status = ProcurementOrderStatus.DRAFT;

    private Date poCreatedDate;
    private Date poSubmittedForApprovalDate;
    private Date poApprovedDate;
    private Date poReleasedDate;

    // ==================== PAYMENT TYPE ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_id")
    private PaymentType paymentType;

    // ==================== AUDIT FIELDS ====================

    private Long createdBy;
    private Long updatedBy;
    private Long approvedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private boolean isDeleted = false;
}