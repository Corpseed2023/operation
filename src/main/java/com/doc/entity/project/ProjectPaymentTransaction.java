package com.doc.entity.project;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Entity
@Table(name = "project_payment_transaction")
@Getter
@Setter
@NoArgsConstructor
public class ProjectPaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique identifier for the transaction")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pid", nullable = false)
    @Comment("Project associated with the transaction")
    private Project project;

    @Column(name = "amt", nullable = false)
    @Comment("Payment amount")
    private Double amount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "pdt", nullable = false)
    @Comment("Payment date")
    private Date paymentDate;

    @Column(name = "cb", nullable = false)
    @Comment("Created by user ID")
    private Long createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdt", updatable = false)
    @Comment("Created date")
    private Date createdDate;
}
