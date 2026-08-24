package com.doc.entity.milestone;

import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "milestone_on_hold_requests",
        indexes = {
                @Index(name = "idx_hold_request_assignment", columnList = "milestone_assignment_id"),
                @Index(name = "idx_hold_request_manager_status", columnList = "approver_id, approval_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MilestoneOnHoldRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_assignment_id", nullable = false)
    private ProjectMilestoneAssignment milestoneAssignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "previous_status_id", nullable = false)
    private MilestoneStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private MilestoneOnHoldApprovalStatus approvalStatus = MilestoneOnHoldApprovalStatus.PENDING;

    @Column(name = "request_reason", nullable = false, length = 1000)
    private String requestReason;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }
}
