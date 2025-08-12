package com.doc.entity.project;

import com.doc.entity.product.Milestone;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.Date;

/**
 * Entity representing the assignment of a milestone to a user for a specific project.
 */
@Entity
@Table(name = "project_milestone_assignment")
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

    @Column(name = "status", nullable = false)
    @Comment("Milestone status: LOCKED, UNLOCKED, IN_PROGRESS, COMPLETED")
    private String status = "LOCKED";

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



}