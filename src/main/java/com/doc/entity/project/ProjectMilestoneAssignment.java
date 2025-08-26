package com.doc.entity.project;

import com.doc.entity.product.Milestone;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.product.ProductRequiredDocuments;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entity representing the assignment of a milestone to a user for a specific project.
 */
@Entity
@Table(name = "project_milestone_assignment", indexes = {
        @Index(name = "idx_project_id", columnList = "project_id"),
        @Index(name = "idx_milestone_id", columnList = "milestone_id"),
        @Index(name = "idx_assigned_user_id", columnList = "assigned_user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_is_visible", columnList = "is_visible")
})
@Getter
@Setter
@NoArgsConstructor
public class ProjectMilestoneAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Assignment ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Comment("Associated project")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_milestone_map_id", nullable = false)
    @Comment("Associated product-milestone mapping")
    private ProductMilestoneMap productMilestoneMap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    @Comment("Associated milestone")
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    @Comment("User assigned to the milestone")
    private User assignedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Comment("Milestone status: NEW, IN_PROGRESS, ON_HOLD, COMPLETED, REJECTED")
    private MilestoneStatus status = MilestoneStatus.NEW;

    @Column(name = "status_reason", length = 1000)
    @Comment("Reason for current status (required for ON_HOLD, REJECTED)")
    private String statusReason;

    @Column(name = "is_visible", nullable = false)
    @Comment("Visibility flag: true if milestone is accessible, false if not")
    private boolean isVisible = false;

    @Column(name = "visibility_reason", length = 1000)
    @Comment("Reason for visibility status (e.g., 'Insufficient payment', 'Previous milestone incomplete')")
    private String visibilityReason;

    @Column(name = "rework_attempts", nullable = false)
    @Comment("Number of rework attempts for this milestone")
    private int reworkAttempts = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Date when the milestone became visible")
    private Date visibleDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Date when the milestone was started (set to IN_PROGRESS)")
    private Date startedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Date when the milestone was completed")
    private Date completedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private LocalDate date;

    @Comment("Created by user ID")
    private Long createdBy;

    @Comment("Updated by user ID")
    private Long updatedBy;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "milestoneAssignment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Comment("Documents uploaded for this milestone")
    private List<ProjectDocumentUpload> documents = new ArrayList<>();
}