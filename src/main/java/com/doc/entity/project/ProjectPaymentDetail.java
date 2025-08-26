package com.doc.entity.project;

import com.doc.entity.client.PaymentType;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "project_payment_detail", indexes = {
        @Index(name = "idx_project_id", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ProjectPaymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique identifier for payment details")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
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
    @Comment("Payment status: PENDING, APPROVED, REJECTED")
    private String paymentStatus = "PENDING";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private LocalDate date;

    @Comment("Created by user ID")
    private Long createdBy;

    @Comment("Updated by user ID")
    private Long updatedBy;

    @Comment("Is deleted flag (soft delete)")
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_id", nullable = false)
    @Comment("Associated payment type")
    private PaymentType paymentType;
}