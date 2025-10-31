package com.doc.entity.document;

import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

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
    @Comment("Primary key: Unique identifier for payment details")
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
    @Comment("Required document for this upload (references ID)")
    private ProductRequiredDocuments requiredDocument;

    @Column(name = "file_url", nullable = false, length = 1000)
    @Comment("URL of the uploaded document")
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    @Comment("Name of the uploaded file")
    private String fileName;

    @Column(name = "old_file_url", length = 1000)
    @Comment("URL of the previous file (if replaced)")
    private String oldFileUrl;

    @Column(name = "old_file_name", length = 255)
    @Comment("Name of the previous file (if replaced)")
    private String oldFileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    @Comment("Document status: Reference to DocumentStatus entity")
    private DocumentStatus status;  // Changed from enum to entity reference

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

    @Column(name = "replacement_count", nullable = false)
    @Comment("Number of times the document has been replaced")
    private int replacementCount = 0;

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