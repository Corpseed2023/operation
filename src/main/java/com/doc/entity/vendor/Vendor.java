package com.doc.entity.vendor;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "vendors", indexes = {
        @Index(name = "idx_vendor_gst_number", columnList = "gstNumber", unique = true),
        @Index(name = "idx_vendor_pan_number", columnList = "panNumber", unique = true),
        @Index(name = "idx_vendor_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    @Comment("Vendor company/name")
    private String name;

    @Column(length = 1000)
    @Comment("Vendor description or remarks")
    private String description;

    @Column(length = 255)
    @Comment("Primary vendor email")
    private String email;

    @Column(length = 20)
    @Comment("Primary vendor mobile number")
    private String mobile;

    @Column(name = "full_address", length = 1000)
    @Comment("Vendor complete address")
    private String fullAddress;

    @Column(name = "city", length = 100)
    @Comment("Vendor city")
    private String city;

    @Column(name = "state", length = 100)
    @Comment("Vendor state")
    private String state;

    @Column(name = "country", length = 100)
    @Comment("Vendor country")
    private String country;

    /**
     * GST is not mandatory during basic vendor creation.
     * It can be collected during onboarding.
     */
    @Column(length = 15, unique = true)
    @Comment("Vendor GST number")
    private String gstNumber;

    @Enumerated(EnumType.STRING)
    private VendorGSTRegistrationType gstRegistrationType;

    /**
     * PAN is not mandatory during basic vendor creation.
     * It can be collected during onboarding.
     */
    @Column(length = 10, unique = true)
    @Comment("Vendor PAN number")
    private String panNumber;

    /**
     * Vendor master status.
     *
     * Default:
     * PROSPECTIVE because vendor is only created with basic details.
     *
     * ACTIVE only after:
     * Procurement Verification -> Legal Approval -> Accounts Approval -> Vendor Activation
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Comment("Vendor master status")
    private VendorStatus status = VendorStatus.PROSPECTIVE;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Record created date")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Record last updated date")
    private Date updatedDate;

    @Comment("User ID who created the vendor")
    private Long createdBy;

    @Comment("User ID who last updated the vendor")
    private Long updatedBy;

    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    @Column(name = "restriction_reason", length = 2000)
    private String restrictionReason;

    @Column(name = "restriction_start_date")
    private LocalDate restrictionStartDate;

    @Column(name = "restriction_end_date")
    private LocalDate restrictionEndDate;

    @Column(name = "restricted_at")
    private LocalDateTime restrictedAt;

    @Column(name = "restricted_by")
    private Long restrictedBy;

    /**
     * Vendor contact persons.
     */
    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorContact> contacts = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();

        if (this.status == null) {
            this.status = VendorStatus.PROSPECTIVE;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedDate = new Date();
    }
}