package com.doc.entity.vendor;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(
        name = "rfq_vendors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rfq_vendor",
                        columnNames = {"rfq_id", "vendor_id"}
                )
        },
        indexes = {
                @Index(name = "idx_rfq_vendor_rfq", columnList = "rfq_id"),
                @Index(name = "idx_rfq_vendor_vendor", columnList = "vendor_id"),
                @Index(name = "idx_rfq_vendor_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class  RFQVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RFQ to which this vendor is invited.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    /**
     * Vendor who received RFQ.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    /**
     * Email/contact used while sending RFQ.
     */
    @Column(length = 255)
    private String sentToEmail;

    @Column(length = 20)
    private String sentToMobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RFQVendorStatus status = RFQVendorStatus.ADDED;

    @Temporal(TemporalType.TIMESTAMP)
    private Date sentDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date quotationReceivedDate;

    @Column(length = 1000)
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