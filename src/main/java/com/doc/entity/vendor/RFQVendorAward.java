package com.doc.entity.vendor;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * RFQVendorAward represents final vendor selection/allocation
 * after quotation comparison.
 *
 * This entity supports:
 * 1. Single vendor selection
 * 2. Multiple vendor selection
 * 3. Item-wise/service-wise allocation
 * 4. Partial quantity allocation
 *
 * Example:
 * RFQ-NBFC-001
 *      -> Balaji selected for NBFC Documentation Support
 *      -> RK Consultant selected for RBI Query Reply Support
 */
@Entity
@Table(
        name = "rfq_vendor_awards",
        indexes = {
                @Index(name = "idx_award_rfq", columnList = "rfq_id"),
                @Index(name = "idx_award_vendor", columnList = "vendor_id"),
                @Index(name = "idx_award_rfq_vendor", columnList = "rfq_vendor_id"),
                @Index(name = "idx_award_quotation", columnList = "quotation_id"),
                @Index(name = "idx_award_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFQVendorAward {

    /**
     * Primary key of RFQ vendor award table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /**
     * RFQ for which vendor is selected.
     *
     * Example:
     * RFQ-NBFC-001
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    /**
     * Vendor mapping inside this RFQ.
     *
     * This tells which vendor was part of this RFQ.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_vendor_id", nullable = false)
    private RFQVendor rfqVendor;

    /**
     * Selected vendor.
     *
     * Example:
     * Balaji Compliance Services.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    /**
     * Vendor quotation selected during comparison.
     *
     * Example:
     * Balaji quotation of Rs. 75,000.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private VendorQuotation quotation;

    /**
     * Quotation item/service for which vendor is awarded.
     *
     * Important:
     * For scalable design, create award item-wise.
     *
     * Example:
     * NBFC Documentation Support
     * RBI Filing Support
     * EPR Recycler Coordination
     * Cement Supply
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_item_id", nullable = false)
    private VendorQuotationItem quotationItem;

    /**
     * Description of awarded work/service/item.
     */
    @Column(length = 1000)
    private String description;

    /**
     * Awarded quantity.
     *
     * Example:
     * Material: 50 Tons
     * Service: 1 Service
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal awardedQuantity = BigDecimal.ZERO;

    /**
     * Unit of awarded item/service.
     *
     * Example:
     * Tons, Kg, Nos, SERVICE, APPLICATION, PROJECT.
     */
    @Column(length = 50)
    private String unit;

    /**
     * Final approved unit rate.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal awardedUnitRate = BigDecimal.ZERO;

    /**
     * Basic awarded amount before tax.
     *
     * Formula:
     * awardedQuantity * awardedUnitRate
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal awardedAmount = BigDecimal.ZERO;

    /**
     * Tax percentage applicable on awarded item/service.
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;

    /**
     * Tax amount on awarded amount.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Final awarded amount including tax.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalAwardedAmount = BigDecimal.ZERO;

    /**
     * Reason why this vendor was selected.
     *
     * Example:
     * Selected due to better NBFC experience and acceptable TAT.
     */
    @Column(length = 1000)
    private String selectionReason;

    /**
     * Internal procurement remarks.
     */
    @Column(length = 1000)
    private String remarks;

    /**
     * Award status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RFQVendorAwardStatus status = RFQVendorAwardStatus.DRAFT;

    /**
     * User ID who selected/awarded this vendor.
     */
    private Long awardedBy;

    /**
     * Date and time when vendor was awarded.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date awardedDate;

    /**
     * User ID who created this award record.
     */
    private Long createdBy;

    /**
     * User ID who last updated this award record.
     */
    private Long updatedBy;

    /**
     * Record creation date.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    /**
     * Last update date.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    /**
     * Soft delete flag.
     */
    private boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();

        if (this.awardedDate == null && this.status == RFQVendorAwardStatus.AWARDED) {
            this.awardedDate = new Date();
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedDate = new Date();

        if (this.awardedDate == null && this.status == RFQVendorAwardStatus.AWARDED) {
            this.awardedDate = new Date();
        }
    }
}