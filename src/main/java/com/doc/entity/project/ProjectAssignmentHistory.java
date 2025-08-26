package com.doc.entity.project;

import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

/**
 * Entity to store the history of project milestone assignments with reasons.
 */
@Entity
@Table(name = "project_assignment_history", indexes = {
        @Index(name = "idx_project_id", columnList = "project_id"),
        @Index(name = "idx_milestone_assignment_id", columnList = "milestone_assignment_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ProjectAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique identifier for the assignment history record")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Comment("Project associated with this assignment")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_assignment_id", nullable = false)
    @Comment("Milestone assignment associated with this history record")
    private ProjectMilestoneAssignment milestoneAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    @Comment("User assigned to the milestone")
    private User assignedUser;

    @Column(name = "assignment_reason", nullable = false)
    @Comment("Reason for assigning this user (e.g., 'Highest rating in round-robin', 'Manager fallback', 'Admin assigned')")
    private String assignmentReason;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Date when the assignment was made")
    private Date createdDate;

    @Comment("User ID who created this history record")
    private Long createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Date when the record was last updated")
    private Date updatedDate;

    @Comment("User ID who last updated this record")
    private Long updatedBy;

    @Comment("Is deleted flag (soft delete)")
    private boolean isDeleted = false;
}