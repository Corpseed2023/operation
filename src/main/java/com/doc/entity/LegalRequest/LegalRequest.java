package com.doc.entity.LegalRequest;

import com.doc.em.LegalStatus;
import com.doc.entity.document.LegalRequestDocument;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
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

    @ManyToOne
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project;

    @ManyToOne
    @JoinColumn(name = "project_milestone_assignment")
    private ProjectMilestoneAssignment projectMilestoneAssignment;

    @Column(name = "tat_in_days", nullable = false)
    private double tatInDays;

    private LegalStatus legalStatus;

    @OneToMany(mappedBy = "legalRequest", cascade = CascadeType.ALL)
    private List<LegalRequestDocument> documents;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "status_reason")
    private String statusReason;
}