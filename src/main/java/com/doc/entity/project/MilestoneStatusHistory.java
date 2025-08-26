package com.doc.entity.project;

import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

/**
 * Entity to store the history of milestone status changes.
 */
@Entity
@Table(name = "milestone_status_history", indexes = {
        @Index(name = "idx_assignment_id", columnList = "milestone_assignment_id")
})
@Getter
@Setter
@NoArgsConstructor
public class MilestoneStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique identifier for the status history record")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_assignment_id", nullable = false)
    @Comment("Associated milestone assignment")
    private ProjectMilestoneAssignment milestoneAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    @Comment("Previous status of the milestone")
    private MilestoneStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    @Comment("New status of the milestone")
    private MilestoneStatus newStatus;

    @Column(name = "change_reason", length = 1000, nullable = false)
    @Comment("Reason for the status change")
    private String changeReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    @Comment("User who changed the status")
    private User changedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    @Comment("Date when the status was changed")
    private Date changeDate;

    @Comment("Is deleted flag (soft delete)")
    private boolean isDeleted = false;
}