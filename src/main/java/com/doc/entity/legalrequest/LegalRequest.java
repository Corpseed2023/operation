package com.doc.entity.legalrequest;

import com.doc.em.LegalStatus;
import com.doc.entity.document.LegalRequestDocument;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "legal_request")
@Getter
@Setter
@NoArgsConstructor
public class LegalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Project for which legal help is requested
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Exact milestone assignment: Document, Filing, Approval, etc.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_milestone_assignment_id", nullable = false)
    private ProjectMilestoneAssignment projectMilestoneAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_status", nullable = false, length = 50)
    private LegalStatus legalStatus = LegalStatus.INITIATED;

    @OneToMany(
            mappedBy = "legalRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<LegalRequestDocument> documents = new ArrayList<>();

    // Legal person assigned to this request
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_legal")
    private User assignedToLegal;

    @Column(name = "legal_request_title", nullable = false, length = 255)
    private String legalRequestTitle;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "status_reason", length = 1000)
    private String statusReason;

    @Column(name = "resolution_summary", length = 2000)
    private String resolutionSummary;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "viewed_by")
    private Long viewedBy;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public void addDocument(LegalRequestDocument document) {
        documents.add(document);
        document.setLegalRequest(this);
    }

    public void removeDocument(LegalRequestDocument document) {
        documents.remove(document);
        document.setLegalRequest(null);
    }
}