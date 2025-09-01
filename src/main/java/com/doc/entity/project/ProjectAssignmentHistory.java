package com.doc.entity.project;

import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

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

    @Column(name = "assignment_reason", nullable = false)
    @Comment("Reason for assigning this user, e.g., Highest rating in round-robin or Manager fallback or Admin assigned")
    private String assignmentReason;

    @Column(name = "created_by")
    @Comment("User ID who created this history record")
    private Long createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date")
    @Comment("Date when the assignment was made")
    private Date createdDate;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Is deleted flag (soft delete)")
    private boolean isDeleted = false;

    @Column(name = "updated_by")
    @Comment("User ID who last updated this record")
    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    @Comment("Date when the record was last updated")
    private Date updatedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    @Comment("User assigned to the milestone")
    private User assignedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_assignment_id", nullable = false)
    @Comment("Milestone assignment associated with this history record")
    private ProjectMilestoneAssignment milestoneAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Comment("Project associated with this assignment")
    private Project project;


}