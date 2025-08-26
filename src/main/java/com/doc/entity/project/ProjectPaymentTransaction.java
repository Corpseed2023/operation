package com.doc.entity.project;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Entity
@Table(name = "project_payment_transaction", indexes = {
        @Index(name = "idx_project_id", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ProjectPaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique identifier for the transaction")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("Project associated with the transaction")
    private Project project;

    @Column(nullable = false)
    @Comment("Payment amount (positive for payment, negative for refund)")
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    @Comment("Type of transaction: PAYMENT or REFUND")
    private TransactionType transactionType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    @Comment("Transaction date")
    private Date transactionDate;

    @Column(nullable = false)
    @Comment("Created by user ID")
    private Long createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    @Comment("Created date")
    private Date createdDate;

    public enum TransactionType {
        PAYMENT,
        REFUND
    }
}