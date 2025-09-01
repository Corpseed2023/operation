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
    @Comment("Primary key: Transaction ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Comment("Associated project")
    private Project project;

    @Column(nullable = false)
    @Comment("Transaction amount")
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    @Comment("Transaction type: PAYMENT, REFUND")
    private TransactionType transactionType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "transaction_date")
    @Comment("Transaction date")
    private Date transactionDate;

    @Comment("Created by user ID")
    private Long createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Created date")
    private Date createdDate;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    public enum TransactionType {
        PAYMENT, REFUND
    }
}