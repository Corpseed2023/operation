package com.doc.entity.vendor;

import com.doc.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(
        name = "vendor_service_mappings",
        indexes = {
                @Index(name = "idx_vendor_service_vendor", columnList = "vendor_id"),
                @Index(name = "idx_vendor_service_product", columnList = "product_id"),
                @Index(name = "idx_vendor_service_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vendor_product_active",
                        columnNames = {"vendor_id", "product_id", "is_deleted"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorServiceMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Approved vendor.
     *
     * Example:
     * Balaji Compliance Services
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    /**
     * Product / service for which vendor is approved.
     *
     * Example:
     * FSSAI License, NBFC Registration, CDSCO, BIS, EPR
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * RFQ award from which this approved rate/service was created.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_finalization_id")
    private VendorFinalization vendorFinalization;

    /**
     * Vendor onboarding from which this mapping was activated.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_onboarding_id")
    private VendorOnboarding vendorOnboarding;

    /**
     * Service category snapshot.
     *
     * Example:
     * FSSAI, NBFC, CDSCO, EPR, LEGAL_CONSULTANT
     */
    @Column(name = "service_category", length = 150)
    private String serviceCategory;

    /**
     * Final approved rate for this product/service.
     */
    @Column(name = "approved_rate", precision = 15, scale = 2)
    private BigDecimal approvedRate = BigDecimal.ZERO;

    /**
     * Tax percentage if applicable.
     */
    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;

    /**
     * Final amount including tax.
     */
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Turnaround time in days.
     */
    @Column(name = "tat_days")
    private Integer tatDays;

    /**
     * Payment terms.
     *
     * Example:
     * 50% advance, 50% after completion.
     */
    @Column(name = "payment_terms", length = 1000)
    private String paymentTerms;

    /**
     * Scope approved for this vendor and product.
     */
    @Column(name = "scope_of_work", length = 2000)
    private String scopeOfWork;

    /**
     * Rate validity start date.
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "valid_from")
    private Date validFrom;

    /**
     * Rate validity end date.
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "valid_to")
    private Date validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private VendorServiceMappingStatus status = VendorServiceMappingStatus.ACTIVE;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    private Date updatedDate;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();

        if (this.approvedRate == null) {
            this.approvedRate = BigDecimal.ZERO;
        }

        if (this.taxPercent == null) {
            this.taxPercent = BigDecimal.ZERO;
        }

        this.totalAmount = this.approvedRate
                .add(this.approvedRate.multiply(this.taxPercent).divide(BigDecimal.valueOf(100)));
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedDate = new Date();

        if (this.approvedRate == null) {
            this.approvedRate = BigDecimal.ZERO;
        }

        if (this.taxPercent == null) {
            this.taxPercent = BigDecimal.ZERO;
        }

        this.totalAmount = this.approvedRate
                .add(this.approvedRate.multiply(this.taxPercent).divide(BigDecimal.valueOf(100)));
    }
}