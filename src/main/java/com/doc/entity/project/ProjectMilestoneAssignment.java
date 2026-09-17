package com.doc.entity.project;

import com.doc.em.CertificateValidityType;
import com.doc.em.CertificationTenureUnit;
import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.milestone.Milestone;
import com.doc.entity.milestone.MilestoneStatus;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(
        name = "project_milestone_assignment",
        indexes = {

                @Index(
                        name = "idx_project_id",
                        columnList = "project_id"
                ),

                @Index(
                        name = "idx_milestone_id",
                        columnList = "milestone_id"
                ),

                @Index(
                        name = "idx_assigned_user_id",
                        columnList = "assigned_user_id"
                ),

                @Index(
                        name = "idx_status_id",
                        columnList = "status_id"
                ),

                @Index(
                        name = "idx_is_visible",
                        columnList = "is_visible"
                ),

                @Index(
                        name = "idx_certificate_validity_type",
                        columnList = "certificate_validity_type"
                ),

                @Index(
                        name = "idx_certificate_expiry_date",
                        columnList = "certificate_expiry_date"
                ),

                @Index(
                        name = "idx_renewal_due_date",
                        columnList = "renewal_due_date"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectMilestoneAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Assignment ID")
    private Long id;

    // =========================================================
    // PROJECT
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "project_id",
            nullable = false
    )
    @Comment("Associated project")
    private Project project;

    // =========================================================
    // PRODUCT MILESTONE MAPPING
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_milestone_map_id",
            nullable = false
    )
    @Comment("Associated product-milestone mapping")
    private ProductMilestoneMap productMilestoneMap;

    // =========================================================
    // MILESTONE
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "milestone_id",
            nullable = false
    )
    @Comment("Associated milestone")
    private Milestone milestone;

    // =========================================================
    // ASSIGNED USER
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    @Comment("User assigned to the milestone")
    private User assignedUser;

    // =========================================================
    // STATUS
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "status_id",
            nullable = false
    )
    @Comment("Milestone status: Reference to MilestoneStatus entity")
    private MilestoneStatus status;

    @Column(
            name = "status_reason",
            length = 1000
    )
    @Comment("Reason for current status")
    private String statusReason;

    // =========================================================
    // VISIBILITY
    // =========================================================

    @Column(
            name = "is_visible",
            nullable = false
    )
    @Comment("Visibility flag")
    private boolean isVisible = false;

    @Column(
            name = "visibility_reason",
            length = 1000
    )
    @Comment("Reason for visibility status")
    private String visibilityReason;

    // =========================================================
    // REWORK
    // =========================================================

    @Column(
            name = "rework_attempts",
            nullable = false
    )
    @Comment("Number of rework attempts")
    private int reworkAttempts = 0;

    // =========================================================
    // MILESTONE DATES
    // =========================================================

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Date when the milestone became visible")
    private Date visibleDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Date when the milestone was started")
    private Date startedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Date when the milestone was completed")
    private Date completedDate;

    // =========================================================
    // CERTIFICATION DETAILS
    // =========================================================

    /**
     * Actual date from which the certificate/license
     * became effective.
     *
     * Example:
     * 01-04-2026
     */
    @Column(name = "certificate_issue_date")
    @Comment("Certificate issue/effective date")
    private LocalDate certificateIssueDate;

    /**
     * Defines whether the certificate has an expiry date
     * or is valid for lifetime.
     *
     * FIXED_TERM:
     * certificateExpiryDate is expected.
     *
     * LIFETIME:
     * certificateExpiryDate and renewalDueDate can remain null.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "certificate_validity_type",
            length = 30
    )
    @Comment("Certificate validity type: FIXED_TERM or LIFETIME")
    private CertificateValidityType certificateValidityType;

    /**
     * Existing field.
     *
     * Example:
     * certificationTenure = 5
     * certificationTenureUnit = YEARS
     *
     * Can remain null for LIFETIME certificates.
     */
    @Column(name = "certification_tenure")
    @Comment("Certificate validity quantity, for example 3")
    private Integer certificationTenure;

    /**
     * Existing field.
     *
     * DAYS
     * MONTHS
     * YEARS
     *
     * Can remain null for LIFETIME certificates.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "certification_tenure_unit",
            length = 20
    )
    @Comment("Certificate validity unit: DAYS, MONTHS or YEARS")
    private CertificationTenureUnit certificationTenureUnit;

    /**
     * Actual expiry date printed on the certificate.
     *
     * Required only when:
     * certificateValidityType = FIXED_TERM
     *
     * Null when:
     * certificateValidityType = LIFETIME
     */
    @Column(name = "certificate_expiry_date")
    @Comment("Valid-till date printed on the certificate")
    private LocalDate certificateExpiryDate;

    /**
     * Existing renewal trigger date.
     *
     * Example:
     * Expiry = 31-03-2031
     * Renewal Due = 01-03-2031
     *
     * This may later be replaced for scheduler purposes by
     * ProjectComplianceSchedule.leadCreationDate.
     */
    @Column(name = "renewal_due_date")
    @Comment("Date from which certificate renewal should begin")
    private LocalDate renewalDueDate;

    /**
     * Certificate document stored in ProjectDocumentUpload.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_document_id")
    @Comment("Certificate attachment from project_document_upload")
    private ProjectDocumentUpload certificateDocument;

    /**
     * Existing external/storage URL for certification document.
     */
    @Column(
            name = "certification_attachment_url",
            length = 2000
    )
    @Comment("Certificate attachment URL")
    private String certificationAttachmentUrl;

    // =========================================================
    // AUDIT
    // =========================================================

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    @Comment("Record creation timestamp")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Record last updated timestamp")
    private Date updatedDate;

    /**
     * Existing general milestone date.
     */
    @Column(name = "date")
    @Comment("General milestone date")
    private LocalDate date;

    @Column(name = "created_by")
    @Comment("Created by user ID")
    private Long createdBy;

    @Column(name = "updated_by")
    @Comment("Updated by user ID")
    private Long updatedBy;

    @Column(
            name = "is_deleted",
            nullable = false
    )
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    // =========================================================
    // ACKNOWLEDGEMENT
    // =========================================================

    @Column(
            name = "acknowledgement_attachment_url",
            length = 2000
    )
    @Comment(
            "Optional acknowledgement/supporting document URL " +
                    "for the latest milestone status update"
    )
    private String acknowledgementAttachmentUrl;

    @Column(
            name = "acknowledgement_attachment_name",
            length = 500
    )
    @Comment(
            "Original/display name of the acknowledgement attachment"
    )
    private String acknowledgementAttachmentName;

}