package com.doc.entity.project;

import com.doc.entity.product.ProductRequiredDocuments;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.GenericGenerator;

import java.util.Date;
import java.util.UUID;

/**
 * Represents the document uploaded for a specific project milestone against a required document item.
 */
@Entity
@Table(name = "project_document_upload", indexes = {
        @Index(name = "idx_project_id", columnList = "project_id"),
        @Index(name = "idx_milestone_assignment_id", columnList = "milestone_assignment_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ProjectDocumentUpload {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    @Comment("Primary key: Unique identifier for the document upload")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Comment("Associated project")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_assignment_id", nullable = false)
    @Comment("Associated milestone assignment")
    private ProjectMilestoneAssignment milestoneAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_document_uuid", nullable = false, referencedColumnName = "uuid")
    @Comment("Required document for this upload (references UUID)")
    private ProductRequiredDocuments requiredDocument;

    @Column(name = "file_url", nullable = false, length = 1000)
    @Comment("URL of the uploaded document")
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Comment("Document status: PENDING, UPLOADED, VERIFIED, REJECTED")
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "remarks", length = 1000)
    @Comment("Remarks for the document status (required for REJECTED)")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    @Comment("User who uploaded the document")
    private User uploadedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "upload_time")
    @Comment("Time when the document was uploaded")
    private Date uploadTime;

    @Column(name = "created_by", nullable = false)
    @Comment("User ID who created the document upload")
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    @Comment("User ID who last updated the document upload")
    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", nullable = false, updatable = false)
    @Comment("Date when the document upload was created")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date", nullable = false)
    @Comment("Date when the document upload was last updated")
    private Date updatedDate;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Is deleted flag (soft delete)")
    private boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = new Date();
    }
}