package com.doc.entity.client;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(
        name = "payment_type",
        indexes = {
                @Index(
                        name = "idx_payment_type_code",
                        columnList = "code",
                        unique = true
                ),
                @Index(
                        name = "idx_payment_type_name",
                        columnList = "name",
                        unique = true
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PaymentType {

    @Id
    @Comment("Primary Key: Unique identifier for payment type")
    private Long id;

    /**
     * Stable payment type code.
     *
     * Must remain aligned with Account Service.
     *
     * FULL
     * PARTIAL
     * INSTALLMENT
     * PURCHASE_ORDER
     */
    @Column(
            name = "code",
            nullable = false,
            unique = true,
            length = 50
    )
    @Comment("Stable payment type code from Account Service")
    private String code;

    /**
     * Human-readable payment type name.
     */
    @Column(
            name = "name",
            nullable = false,
            unique = true,
            length = 150
    )
    @Comment("Display name of payment type")
    private String name;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    private Date updatedDate;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {

        Date now = new Date();

        if (createdDate == null) {
            createdDate = now;
        }

        updatedDate = now;

        if (date == null) {
            date = LocalDate.now();
        }

        if (code != null) {
            code = code.trim().toUpperCase();
        }

        if (name != null) {
            name = name.trim();
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedDate = new Date();

        if (code != null) {
            code = code.trim().toUpperCase();
        }

        if (name != null) {
            name = name.trim();
        }
    }
}