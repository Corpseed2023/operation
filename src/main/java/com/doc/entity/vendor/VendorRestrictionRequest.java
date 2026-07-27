package com.doc.entity.vendor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "vendor_restriction_requests",
        indexes = {
                @Index(
                        name = "idx_vendor_restriction_vendor",
                        columnList = "vendor_id"
                ),
                @Index(
                        name = "idx_vendor_restriction_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorRestrictionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Vendor against which suspension or blacklist is requested.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    /*
     * SUSPENSION or BLACKLIST.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "restriction_type", nullable = false, length = 30)
    private VendorRestrictionType restrictionType;

    /*
     * Current approval workflow status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private VendorRestrictionRequestStatus status;

    @Column(name = "reason", nullable = false, length = 2000)
    private String reason;

    /*
     * Required for suspension.
     * Not required for blacklist.
     */
    @Column(name = "restriction_start_date")
    private LocalDate restrictionStartDate;

    @Column(name = "restriction_end_date")
    private LocalDate restrictionEndDate;

    /*
     * User who created the request.
     */
    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /*
     * Accounts approval/rejection details.
     */
    @Column(name = "accounts_reviewed_by")
    private Long accountsReviewedBy;

    @Column(name = "accounts_reviewed_at")
    private LocalDateTime accountsReviewedAt;

    @Column(name = "accounts_remarks", length = 1000)
    private String accountsRemarks;

    /*
     * Admin final approval/rejection details.
     */
    @Column(name = "admin_reviewed_by")
    private Long adminReviewedBy;

    @Column(name = "admin_reviewed_at")
    private LocalDateTime adminReviewedAt;

    @Column(name = "admin_remarks", length = 1000)
    private String adminRemarks;

    /*
     * Optional supporting document.
     */
    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;

    /*
     * For concurrent approval protection.
     */
    @Version
    private Long version;
}
