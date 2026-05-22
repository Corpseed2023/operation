package com.doc.entity.vendor;

import com.doc.entity.client.PaymentType;
import com.doc.entity.project.ProcurementMilestoneAssignment;
import com.doc.entity.project.Project;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    @Comment("PO Number - e.g. PO-2026-0789")
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procurement_assignment_id", nullable = false)
    private ProcurementMilestoneAssignment procurementAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    private BigDecimal totalAmount;
    private BigDecimal gstAmount;
    private BigDecimal grandTotal;

    @Column(length = 2000)
    private String scopeOfWork;

    private LocalDate issueDate;
    private LocalDate validTillDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_id")
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    private POStatus status = POStatus.DRAFT;

    @Column(length = 2000)
    private String termsAndConditions;

    @ManyToOne(fetch = FetchType.LAZY)
    private User approvedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date approvedDate;

    private Long createdBy;
    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private boolean isDeleted = false;
}