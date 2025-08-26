package com.doc.entity.project;

import com.doc.entity.product.ProductRequiredDocuments;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique identifier for the document upload")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Comment("Associated project")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_assignment_id", nullable = false)
    @Comment("Associated milestone assignment")
    private ProjectMilestoneAssignment milestoneAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_document_id", nullable = false)
    @Comment("Required document for this upload")
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
    @JoinColumn(name = "uploaded_by")
    @Comment("User who uploaded the document")
    private User uploadedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "upload_time")
    @Comment("Time when the document was uploaded")
    private Date uploadTime;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Is deleted flag (soft delete)")
    private boolean isDeleted = false;

    public enum DocumentStatus {
        PENDING,   // Document not yet uploaded
        UPLOADED,  // Document uploaded, awaiting verification
        VERIFIED,  // Document verified
        REJECTED   // Document rejected (requires remarks)
    }
}