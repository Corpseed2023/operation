package com.doc.entity.project;

import com.doc.entity.client.PaymentType;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProjectPaymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary Key: Unique identifier for payment details")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pid", nullable = false, unique = true)
    @Comment("Associated project")
    private Project project;

    @Column(name = "total_amount", nullable = false)
    @Comment("Total project amount")
    private double totalAmount;

    @Column(name = "due_amount", nullable = false)
    @Comment("Remaining unpaid amount")
    private double dueAmount;

    @Column(name = "estimate_id")
    @Comment("Estimate ID from lead service")
    private Long estimateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @Comment("User who approved payment")
    private User approvedBy;

    @Column(name = "payment_status", nullable = false)
    @Comment("Payment Status: PENDING, APPROVED, REJECTED")
    private String paymentStatus = "PENDING";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdt", updatable = false)
    @Comment("Created Date")
    private Date createdDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "udt")
    @Comment("Updated Date")
    private Date updatedDate;

    @Column(name = "cb")
    @Comment("Created By User ID")
    private Long createdBy;

    @Column(name = "ub")
    @Comment("Updated By User ID")
    private Long updatedBy;

    @Column(name = "isd", nullable = false)
    @Comment("Is Deleted flag (soft delete)")
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_id", nullable = false)
    @Comment("Associated Payment Type")
    private PaymentType paymentType;



    }