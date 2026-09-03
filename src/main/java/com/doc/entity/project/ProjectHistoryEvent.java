package com.doc.entity.project;

import com.doc.em.ProjectHistoryActorType;
import com.doc.em.ProjectHistoryEventType;
import com.doc.em.ProjectHistoryReferenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "project_history_event",
        indexes = {
                @Index(
                        name = "idx_project_history_project_date",
                        columnList = "project_id, occurred_at"
                ),
                @Index(
                        name = "idx_project_history_milestone",
                        columnList = "milestone_assignment_id"
                ),
                @Index(
                        name = "idx_project_history_reference",
                        columnList = "reference_type, reference_id"
                ),
                @Index(
                        name = "idx_project_history_event_type",
                        columnList = "event_type"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectHistoryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_assignment_id")
    private ProjectMilestoneAssignment milestoneAssignment;

    /* Snapshot retained even if the milestone is later renamed. */
    @Column(name = "milestone_name", length = 255)
    private String milestoneName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private ProjectHistoryEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 100)
    private ProjectHistoryReferenceType referenceType;

    /* ID of the legal request, document, PO, payment request, etc. */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "event_title", nullable = false, length = 255)
    private String eventTitle;

    @Column(name = "description", length = 3000)
    private String description;

    /* Mandatory business reason where the action requires one. */
    @Column(name = "reason", length = 2000)
    private String reason;

    /* Status, assignee, priority, or other value before the action. */
    @Column(name = "previous_value", length = 1000)
    private String previousValue;

    /* Status, assignee, priority, or other value after the action. */
    @Column(name = "new_value", length = 1000)
    private String newValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 50)
    private ProjectHistoryActorType actorType;

    /* User snapshot: history remains readable after rename or deletion. */
    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    @Column(name = "performed_by_name", nullable = false, length = 255)
    private String performedByName;

    /* For automatic actions, this identifies the user who triggered them. */
    @Column(name = "triggered_by_user_id")
    private Long triggeredByUserId;

    @Column(name = "triggered_by_name", length = 255)
    private String triggeredByName;

    @Column(name = "previous_assignee_id")
    private Long previousAssigneeId;

    @Column(name = "previous_assignee_name", length = 255)
    private String previousAssigneeName;

    @Column(name = "new_assignee_id")
    private Long newAssigneeId;

    @Column(name = "new_assignee_name", length = 255)
    private String newAssigneeName;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }

        if (actorType == null) {
            actorType = ProjectHistoryActorType.USER;
        }

        if (performedByName == null || performedByName.isBlank()) {
            performedByName = "System";
        }
    }
}
