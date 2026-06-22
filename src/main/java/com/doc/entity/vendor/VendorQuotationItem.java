package com.doc.entity.vendor;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * VendorQuotationItem represents item-wise/service-wise quotation details.
 *
 * Works for:
 * 1. Material procurement: Cement, Steel Bars, Machinery
 * 2. Compliance services: FSSAI Filing, CDSCO Documentation, EPR Filing, BIS Testing
 */
@Entity
@Table(
        name = "vendor_quotation_items",
        indexes = {
                @Index(name = "idx_quotation_item_quotation", columnList = "quotation_id"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorQuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent quotation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private VendorQuotation quotation;

    /**
     * Type of item/service.
     *
     * Example:
     * MATERIAL, SERVICE, LEGAL, TESTING_LAB, RECYCLER
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private QuotationItemType itemType = QuotationItemType.SERVICE;

    /**
     * Display order of quotation item.
     */
    private Integer sequenceNo = 1;

    /**
     * Item/service name.
     *
     * Example:
     * Cement, Steel Bars, FSSAI Filing, CDSCO Documentation, EPR Recycler Coordination.
     */
    @Column(nullable = false, length = 255)
    private String itemName;

    /**
     * Item/service description.
     */
    @Column(length = 1000)
    private String description;

    /**
     * Quantity quoted by vendor.
     *
     * For service procurement, usually 1.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    /**
     * Unit of measurement.
     *
     * Material example: Tons, Kg, Bags, Nos
     * Service example: SERVICE, LOT, APPLICATION, LICENSE, PROJECT
     */
    @Column(length = 50)
    private String unit;

    /**
     * Rate per unit.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal unitRate = BigDecimal.ZERO;

    /**
     * Basic amount before tax.
     *
     * Formula:
     * quantity * unitRate
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * GST/tax percentage.
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;

    /**
     * Tax amount.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Final amount including tax.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Additional remarks.
     */
    @Column(length = 500)
    private String remarks;

    private Long createdBy;
    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedDate = new Date();
    }
}