package com.doc.entity.project;

import com.doc.em.CertificateValidityType;
import com.doc.em.CertificationTenureUnit;
import com.doc.em.MilestoneCompletionSource;
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
import java.time.LocalDateTime;
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
                ),

                /*
                 * Scheduler lookup index.
                 */
                @Index(
                        name = "idx_renewal_lead_scheduler",
                        columnList =
                                "renewal_due_date, renewal_lead_created, is_deleted"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectMilestoneAssignment {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Comment("Primary key: Assignment ID")
    private Long id;


    // =========================================================
    // PROJECT
    // =========================================================

    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "project_id",
            nullable = false
    )
    @Comment("Associated project")
    private Project project;


    // =========================================================
    // PRODUCT MILESTONE MAPPING
    // =========================================================

    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "product_milestone_map_id",
            nullable = false
    )
    @Comment("Associated product-milestone mapping")
    private ProductMilestoneMap productMilestoneMap;


    // =========================================================
    // MILESTONE
    // =========================================================

    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "milestone_id",
            nullable = false
    )
    @Comment("Associated milestone")
    private Milestone milestone;


    // =========================================================
    // ASSIGNED USER
    // =========================================================

    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "assigned_user_id"
    )
    @Comment("User assigned to the milestone")
    private User assignedUser;


    // =========================================================
    // STATUS
    // =========================================================

    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "status_id",
            nullable = false
    )
    @Comment(
            "Milestone status: Reference to MilestoneStatus entity"
    )
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

    @Column(name = "return_lead_created", nullable = false)
    @Comment("True once a certificate return lead has been created")
    private boolean returnLeadCreated = false;

    @Column(name = "return_lead_created_at")
    @Comment("Timestamp when the certificate return lead was created")
    private LocalDateTime returnLeadCreatedAt;


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

    @Temporal(
            TemporalType.TIMESTAMP
    )
    @Comment(
            "Date when the milestone became visible"
    )
    private Date visibleDate;


    @Temporal(
            TemporalType.TIMESTAMP
    )
    @Comment(
            "Date when the milestone was started"
    )
    private Date startedDate;


    @Temporal(
            TemporalType.TIMESTAMP
    )
    @Comment(
            "Date when the milestone was completed"
    )
    private Date completedDate;


    // =========================================================
    // CERTIFICATION DETAILS
    // =========================================================

    /**
     * Actual date from which the certificate/license
     * became effective.
     *
     * Example:
     *
     * 01-04-2026
     */
    @Column(
            name = "certificate_issue_date"
    )
    @Comment(
            "Certificate issue/effective date"
    )
    private LocalDate certificateIssueDate;


    /**
     * FIXED_TERM / LIFETIME.
     */
    @Enumerated(
            EnumType.STRING
    )
    @Column(
            name = "certificate_validity_type",
            length = 30
    )
    @Comment(
            "Certificate validity type: FIXED_TERM or LIFETIME"
    )
    private CertificateValidityType certificateValidityType;


    /**
     * Example:
     *
     * 5 YEARS
     */
    @Column(
            name = "certification_tenure"
    )
    @Comment(
            "Certificate validity quantity, for example 3"
    )
    private Integer certificationTenure;


    @Enumerated(
            EnumType.STRING
    )
    @Column(
            name = "certification_tenure_unit",
            length = 20
    )
    @Comment(
            "Certificate validity unit: DAYS, MONTHS or YEARS"
    )
    private CertificationTenureUnit certificationTenureUnit;


    /**
     * Actual expiry date printed on certificate.
     *
     * Required for FIXED_TERM.
     *
     * Null for LIFETIME.
     */
    @Column(
            name = "certificate_expiry_date"
    )
    @Comment(
            "Valid-till date printed on the certificate"
    )
    private LocalDate certificateExpiryDate;


    /**
     * This is the date on which Operation Service
     * will attempt to create the renewal Lead.
     *
     * Example:
     *
     * Certificate Expiry:
     * 01-01-2027
     *
     * Renewal Lead Days:
     * 40
     *
     * Renewal Due:
     * 22-11-2026
     */
    @Column(
            name = "renewal_due_date"
    )
    @Comment(
            "Date from which certificate renewal lead should be created"
    )
    private LocalDate renewalDueDate;


    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "certificate_document_id"
    )
    @Comment(
            "Certificate attachment from project_document_upload"
    )
    private ProjectDocumentUpload certificateDocument;


    @Column(
            name = "certification_attachment_url",
            length = 2000
    )
    @Comment(
            "Certificate attachment URL"
    )
    private String certificationAttachmentUrl;


    // =========================================================
    // CERTIFICATE RETURN
    // =========================================================

    /**
     * YES / NO.
     *
     * Set when the Certification milestone is completed.
     */
    @Column(
            name = "certificate_return",
            length = 3
    )
    @Comment(
            "Certificate return: YES or NO"
    )
    private String certificateReturn;


    /**
     * WEEKLY / MONTHLY / QUARTERLY / YEARLY.
     *
     * Only populated when certificateReturn = YES.
     */
    @Column(
            name = "return_tenure",
            length = 20
    )
    @Comment(
            "Return tenure: WEEKLY, MONTHLY, QUARTERLY or YEARLY"
    )
    private String returnTenure;


    /**
     * Only populated when certificateReturn = YES.
     */
    @Column(
            name = "return_tenure_expiration_date"
    )
    @Comment(
            "Return tenure expiration date"
    )
    private LocalDate returnTenureExpirationDate;


    // =========================================================
    // RENEWAL LEAD
    // =========================================================

    /**
     * false:
     *
     * Renewal Lead is not created yet.
     *
     * true:
     *
     * Lead Service has successfully created/returned
     * the renewal Lead.
     */
    @Column(
            name = "renewal_lead_created",
            nullable = false
    )
    @Comment(
            "True once a renewal lead has been created in Lead Service"
    )
    private boolean renewalLeadCreated = false;


    /**
     * Lead ID returned by Lead Service.
     */
    @Column(
            name = "renewal_lead_id"
    )
    @Comment(
            "Renewal Lead ID returned by Lead Service"
    )
    private Long renewalLeadId;


    /**
     * Successful creation time.
     */
    @Column(
            name = "renewal_lead_created_at"
    )
    @Comment(
            "Timestamp when renewal Lead was successfully created"
    )
    private LocalDateTime renewalLeadCreatedAt;


    /**
     * Number of attempts Operation Service made.
     */
    @Column(
            name = "renewal_lead_attempt_count",
            nullable = false
    )
    @Comment(
            "Number of attempts made to create renewal Lead"
    )
    private Integer renewalLeadAttemptCount = 0;


    /**
     * Last scheduler/API attempt time.
     */
    @Column(
            name = "renewal_lead_last_attempt_at"
    )
    @Comment(
            "Last attempt timestamp for renewal Lead creation"
    )
    private LocalDateTime renewalLeadLastAttemptAt;


    /**
     * Last Lead Service error.
     *
     * It becomes null after successful creation.
     */
    @Column(
            name = "renewal_lead_error",
            length = 1000
    )
    @Comment(
            "Last error received while creating renewal Lead"
    )
    private String renewalLeadError;


    // =========================================================
    // AUDIT
    // =========================================================

    @Temporal(
            TemporalType.TIMESTAMP
    )
    @Column(
            updatable = false
    )
    @Comment(
            "Record creation timestamp"
    )
    private Date createdDate;


    @Temporal(
            TemporalType.TIMESTAMP
    )
    @Comment(
            "Record last updated timestamp"
    )
    private Date updatedDate;


    @Column(
            name = "date"
    )
    @Comment(
            "General milestone date"
    )
    private LocalDate date;


    @Column(
            name = "created_by"
    )
    @Comment(
            "Created by user ID"
    )
    private Long createdBy;


    @Column(
            name = "updated_by"
    )
    @Comment(
            "Updated by user ID"
    )
    private Long updatedBy;


    @Column(
            name = "is_deleted",
            nullable = false
    )
    @Comment(
            "Soft delete flag"
    )
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
            "Original/display name of acknowledgement attachment"
    )
    private String acknowledgementAttachmentName;


    // =========================================================
    // COMPLETION SOURCE
    // =========================================================

    @Enumerated(
            EnumType.STRING
    )
    @Column(
            name = "completion_source",
            length = 30
    )
    private MilestoneCompletionSource completionSource;


    @Column(
            name = "completion_remark",
            length = 1000
    )
    private String completionRemark;


    @Column(
            name = "client_completion_date"
    )
    private LocalDate clientCompletionDate;
}