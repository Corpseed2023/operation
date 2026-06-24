    package com.doc.entity.vendor;
    
    import jakarta.persistence.*;
    import lombok.*;
    
    import java.math.BigDecimal;
    import java.util.Date;
    
    /**
     * VendorFinalization represents final vendor selection/allocation
     * after quotation comparison.
     *
     * This entity supports:
     * 1. Single vendor finalization
     * 2. Multiple vendor finalization
     * 3. Item-wise/service-wise finalization
     * 4. Partial quantity allocation
     *
     * Example:
     * RFQ-NBFC-001
     *      -> Balaji finalized for NBFC Documentation Support
     *      -> RK Consultant finalized for RBI Query Reply Support
     */
    @Entity
    @Table(
            name = "vendor_finalizations",
            indexes = {
                    @Index(name = "idx_finalization_rfq", columnList = "rfq_id"),
                    @Index(name = "idx_finalization_vendor", columnList = "vendor_id"),
                    @Index(name = "idx_finalization_rfq_vendor", columnList = "rfq_vendor_id"),
                    @Index(name = "idx_finalization_quotation", columnList = "quotation_id"),
                    @Index(name = "idx_finalization_status", columnList = "status")
            }
    )
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public class VendorFinalization {
    
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    
        /**
         * RFQ for which vendor is finalized.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "rfq_id", nullable = false)
        private RFQ rfq;
    
        /**
         * Vendor mapping inside this RFQ.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "rfq_vendor_id", nullable = false)
        private RFQVendor rfqVendor;
    
        /**
         * Finalized vendor.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "vendor_id", nullable = false)
        private Vendor vendor;
    
        /**
         * Selected quotation during comparison.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "quotation_id", nullable = false)
        private VendorQuotation quotation;
    
        /**
         * Quotation item/service for which vendor is finalized.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "quotation_item_id", nullable = false)
        private VendorQuotationItem quotationItem;
    
        /**
         * Description of finalized work/service/item.
         */
        @Column(length = 1000)
        private String description;
    
        /**
         * Finalized quantity.
         *
         * Example:
         * Material: 50 Tons
         * Service: 1 Service
         */
        @Column(precision = 15, scale = 2)
        private BigDecimal finalizedQuantity = BigDecimal.ZERO;
    
        /**
         * Unit of finalized item/service.
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
        private BigDecimal finalizedUnitRate = BigDecimal.ZERO;
    
        /**
         * Basic finalized amount before tax.
         *
         * Formula:
         * finalizedQuantity * finalizedUnitRate
         */
        @Column(precision = 15, scale = 2)
        private BigDecimal finalizedAmount = BigDecimal.ZERO;
    
        /**
         * Tax percentage applicable on finalized item/service.
         */
        @Column(precision = 5, scale = 2)
        private BigDecimal taxPercent = BigDecimal.ZERO;
    
        /**
         * Tax amount on finalized amount.
         */
        @Column(precision = 15, scale = 2)
        private BigDecimal taxAmount = BigDecimal.ZERO;
    
        /**
         * Final amount including tax.
         */
        @Column(precision = 15, scale = 2)
        private BigDecimal totalFinalizedAmount = BigDecimal.ZERO;
    
        /**
         * Reason why this vendor was finalized.
         *
         * Example:
         * Better experience, acceptable TAT and reasonable price.
         */
        @Column(length = 1000)
        private String finalizationReason;
    
        /**
         * Internal procurement remarks.
         */
        @Column(length = 1000)
        private String remarks;
    
        /**
         * Finalization status.
         */
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 50)
        private VendorFinalizationStatus status = VendorFinalizationStatus.DRAFT;
    
        /**
         * User ID who finalized this vendor.
         */
        private Long finalizedBy;
    
        /**
         * Date and time when vendor was finalized.
         */
        @Temporal(TemporalType.TIMESTAMP)
        private Date finalizedDate;
    
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
    
            if (this.finalizedDate == null && this.status == VendorFinalizationStatus.FINALIZED) {
                this.finalizedDate = new Date();
            }
        }
    
        @PreUpdate
        public void onUpdate() {
            this.updatedDate = new Date();
    
            if (this.finalizedDate == null && this.status == VendorFinalizationStatus.FINALIZED) {
                this.finalizedDate = new Date();
            }
        }
    }