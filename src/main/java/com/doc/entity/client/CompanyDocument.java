package com.doc.entity.client;

import com.doc.entity.document.DocumentStatus;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Entity
@Table(
        name = "company_documents",
        indexes = {
                @Index(name = "idx_company_id", columnList = "company_id"),
                @Index(name = "idx_required_document_id", columnList = "required_document_id"),
                @Index(name = "idx_status_id", columnList = "status_id"),
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
@Comment("Stores verified documents at company level for reuse across all projects")
public class CompanyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique ID for company-level document")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @Comment("Associated company (e.g., Microsoft)")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_document_id", nullable = false)
    @Comment("Reference to the required document type (e.g., PAN, EPR Authorization)")
    private ProductRequiredDocuments requiredDocument;

    @Column(name = "file_url", nullable = false, length = 1000)
    @Comment("S3 URL of the uploaded document (shared across projects)")
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    @Comment("Original file name (sanitized)")
    private String fileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    @Comment("Document status: PENDING → UPLOADED → VERIFIED")
    private DocumentStatus status;

    @Column(name = "remarks", length = 1000)
    @Comment("Remarks (required if REJECTED)")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable =false)
    @Comment("User who uploaded the document (CRT/Admin)")
    private User uploadedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "upload_time", nullable = false)
    @Comment("Timestamp when document was uploaded")
    private Date uploadTime;

    @Column(name = "created_by", nullable = false)
    @Comment("User ID who created this record")
    private Long createdBy;

    @Column(name = "updated_by")
    @Comment("User ID who last updated this record")
    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", nullable = false, updatable = false)
    @Comment("Record creation timestamp")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    @Comment("Record update timestamp")
    private Date updatedDate;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    @Column(name = "replacement_count", nullable = false)
    @Comment("Number of times this document was replaced")
    private int replacementCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    @Comment("User who verified the document (nullable until verified)")
    private User verifiedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "verified_date")
    @Comment("Date when document was marked VERIFIED")
    private Date verifiedDate;

    // Optional: Expiry date for documents like CTO, EPR
    @Temporal(TemporalType.DATE)
    @Column(name = "expiry_date")
    @Comment("Document expiry date (e.g., CTO, Lab Report)")
    private Date expiryDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();
        this.uploadTime = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = new Date();
    }
}