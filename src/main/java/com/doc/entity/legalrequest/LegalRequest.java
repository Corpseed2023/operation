package com.doc.entity.legalrequest;   // Better package structure

import com.doc.em.LegalStatus;
import com.doc.entity.document.LegalRequestDocument;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_milestone_assignment_id", nullable = false)   // Better column name
    private ProjectMilestoneAssignment projectMilestoneAssignment;

    @Column(name = "tat_in_days", nullable = false)
    private Double tatInDays;

    @Column(name = "tat_reason", length = 500)
    private String tatReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_status", nullable = false)
    private LegalStatus legalStatus;

    @OneToMany(mappedBy = "legalRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LegalRequestDocument> documents = new ArrayList<>();   // Initialize + orphanRemoval

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "status_reason", length = 1000)
    private String statusReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_legal")
    private User assignedToLegal;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "legal_request_title", nullable = false, length = 255)
    private String legalRequestTitle;

    @Column(name = "viewed_by")
    private Long viewedBy;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;


}