package com.doc.entity.document;

import com.doc.entity.client.Company;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.Calendar;
import java.util.Date;

@Entity
@Table(
        name = "company_documents",
        indexes = {
                @Index(name = "idx_company_id", columnList = "company_id"),
                @Index(name = "idx_required_document_id", columnList = "required_document_id"),
                @Index(name = "idx_status_id", columnList = "status_id"),
                @Index(name = "idx_expiry_date", columnList = "expiry_date"),
                @Index(name = "idx_company_doc_unique", columnList = "company_id, required_document_id", unique = true)
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uc_company_required_doc",
                columnNames = {"company_id", "required_document_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@Comment("Company-level reusable documents with MNC compliance")
public class CompanyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_document_id", nullable = false)
    private ProductRequiredDocuments requiredDocument;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    // === NEW: TRACK PREVIOUS FILE (FOR REPLACEMENT) ===
    @Column(name = "old_file_url", length = 1000)
    @Comment("URL of the previous file when replaced")
    private String oldFileUrl;

    @Column(name = "old_file_name", length = 255)
    @Comment("Name of the previous file when replaced")
    private String oldFileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private DocumentStatus status;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "upload_time", nullable = false)
    private Date uploadTime;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", nullable = false, updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date", nullable = false)
    private Date updatedDate;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "replacement_count", nullable = false)
    private int replacementCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "verified_date")
    private Date verifiedDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "expiry_date")
    @Comment("NULL for FIXED docs")
    private Date expiryDate;

    @Column(name = "expiry_set_by")
    private Long expirySetBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiry_set_date")
    private Date expirySetDate;

    @Column(name = "is_permanent", nullable = false)
    private boolean isPermanent = false;

    @Column(name = "renewal_cycle_months")
    private Integer renewalCycleMonths;

    @Temporal(TemporalType.DATE)
    @Column(name = "last_renewal_date")
    private Date lastRenewalDate;

    @Column(name = "file_size_kb", nullable = false)
    private Integer fileSizeKb = 0;

    @Column(name = "file_format", length = 10, nullable = false)
    private String fileFormat = "";

    @Column(name = "validation_passed", nullable = false)
    private boolean validationPassed = false;

    @Column(name = "validation_issues", length = 1000)
    private String validationIssues;

    @Column(name = "quality_score", columnDefinition = "DECIMAL(3,2)")
    private Double qualityScore = 0.0;

    @PrePersist
    protected void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();
        this.uploadTime = new Date();
        if (this.expiryDate != null && this.expirySetDate == null) {
            this.expirySetDate = new Date();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = new Date();
    }

    public boolean isExpired() {
        if (isPermanent || expiryDate == null) return false;
        return expiryDate.before(new Date());
    }


    public boolean isReusable() {
        return "VERIFIED".equals(status.getName()) && !isExpired();
    }

    public int getDaysUntilExpiry() {
        if (expiryDate == null || isPermanent) return Integer.MAX_VALUE;
        long diff = expiryDate.getTime() - System.currentTimeMillis();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }
}