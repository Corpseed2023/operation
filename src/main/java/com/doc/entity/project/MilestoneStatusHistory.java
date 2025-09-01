package com.doc.entity.project;

import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Entity
@Table(name = "milestone_status_history", indexes = {
        @Index(name = "idx_milestone_assignment_id", columnList = "milestone_assignment_id")
})
@Getter
@Setter
@NoArgsConstructor
public class MilestoneStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: History ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_assignment_id", nullable = false)
    @Comment("Associated milestone assignment")
    private ProjectMilestoneAssignment milestoneAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    @Comment("Previous milestone status")
    private MilestoneStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    @Comment("New milestone status")
    private MilestoneStatus newStatus;

    @Column(name = "change_reason", length = 1000)
    @Comment("Reason for status change")
    private String changeReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    @Comment("User who changed the status")
    private User changedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Date of status change")
    private Date changeDate;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;
}