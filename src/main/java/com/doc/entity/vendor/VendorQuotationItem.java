package com.doc.entity.vendor;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
                @Index(name = "idx_quotation_item_type", columnList = "item_type"),
                @Index(name = "idx_quotation_item_deleted", columnList = "is_deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorQuotationItem {

    /**
     * Primary key of vendor quotation item table.
     */
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
    @Column(name = "item_type", length = 50)
    private QuotationItemType itemType = QuotationItemType.SERVICE;

    /**
     * Display order of quotation item.
     */
    @Column(name = "sequence_no")
    private Integer sequenceNo = 1;

    /**
     * Item/service name.
     *
     * Example:
     * Cement, Steel Bars, FSSAI Filing, CDSCO Documentation, EPR Recycler Coordination.
     */
    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    /**
     * Item/service description.
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Quantity quoted by vendor.
     *
     * For service procurement, usually 1.
     */
    @Column(name = "quantity", precision = 15, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    /**
     * Unit of measurement.
     *
     * Material example: Tons, Kg, Bags, Nos
     * Service example: SERVICE, LOT, APPLICATION, LICENSE, PROJECT
     */
    @Column(name = "unit", length = 50)
    private String unit;

    /**
     * Rate per unit.
     */
    @Column(name = "unit_rate", precision = 15, scale = 2)
    private BigDecimal unitRate = BigDecimal.ZERO;

    /**
     * Basic amount before tax.
     *
     * Formula:
     * quantity * unitRate
     */
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * GST/tax percentage.
     */
    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;

    /**
     * Tax amount.
     */
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Final amount including tax.
     */
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Additional remarks.
     */
    @Column(name = "remarks", length = 500)
    private String remarks;

    /**
     * User ID who created quotation item.
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * User ID who last updated quotation item.
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * Record creation date.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    private Date createdDate;

    /**
     * Last update date.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    private Date updatedDate;

    /**
     * Soft delete flag.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();

        calculateAmounts();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedDate = new Date();

        calculateAmounts();
    }

    /**
     * Calculates item-wise amount, tax amount and total amount.
     *
     * Formula:
     * amount = quantity * unitRate
     * taxAmount = amount * taxPercent / 100
     * totalAmount = amount + taxAmount
     */
    public void calculateAmounts() {
        if (this.quantity == null) {
            this.quantity = BigDecimal.ZERO;
        }

        if (this.unitRate == null) {
            this.unitRate = BigDecimal.ZERO;
        }

        if (this.taxPercent == null) {
            this.taxPercent = BigDecimal.ZERO;
        }

        this.amount = this.quantity
                .multiply(this.unitRate)
                .setScale(2, RoundingMode.HALF_UP);

        this.taxAmount = this.amount
                .multiply(this.taxPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        this.totalAmount = this.amount
                .add(this.taxAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}