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
        @Index(name = "idx_milestone_assignment_id", columnList = "milestone_assignment_id"),
        @Index(name = "idx_status_id", columnList = "status_id"),
        @Index(name = "idx_company_source", columnList = "is_from_company_doc")
})
@Getter
@Setter
@NoArgsConstructor
@Comment("Documents uploaded per project milestone with reuse tracking")
public class ProjectDocumentUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_assignment_id", nullable = false)
    private ProjectMilestoneAssignment milestoneAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_document_id", nullable = false)
    private ProductRequiredDocuments requiredDocument;

    @Column(name = "company_doc_source_id")
    private Long companyDocSourceId;

    @Column(name = "is_from_company_doc", nullable = false)
    private boolean isFromCompanyDoc = false;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "old_file_url", length = 1000)
    private String oldFileUrl;

    @Column(name = "old_file_name", length = 255)
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
    @Column(name = "upload_time")
    private Date uploadTime;

    @Temporal(TemporalType.DATE)
    @Column(name = "expiry_date")
    private Date expiryDate;

    @Column(name = "is_permanent", nullable = false)
    private boolean isPermanent = false;

    @Column(name = "is_expired", nullable = false)
    private boolean isExpired = false;

    @Column(name = "file_size_kb", nullable = false)
    private Integer fileSizeKb;

    @Column(name = "file_format", length = 10, nullable = false)
    private String fileFormat;

    @Column(name = "validation_passed", nullable = false)
    private boolean validationPassed = false;

    @Column(name = "validation_issues", length = 1000)
    private String validationIssues;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
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

    @PrePersist
    protected void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();
        if (this.uploadTime == null) this.uploadTime = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = new Date();
    }

    public boolean requiresRenewal() {
        return isExpired && !isPermanent;
    }
}