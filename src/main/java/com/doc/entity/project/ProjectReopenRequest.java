package com.doc.entity.project;

import com.doc.em.ProjectReopenRequestStatus;
import com.doc.entity.department.Department;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(
        name = "project_reopen_request",
        indexes = {
                @Index(name = "idx_reopen_project_id", columnList = "project_id"),
                @Index(name = "idx_reopen_status", columnList = "status"),
                @Index(name = "idx_reopen_requested_by", columnList = "requested_by_id"),
                @Index(name = "idx_reopen_requester_manager", columnList = "requester_manager_id"),
                @Index(name = "idx_reopen_responsible_manager", columnList = "responsible_manager_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectReopenRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Project which will be reopened after approval.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /*
     * Current milestone where mistake was found.
     * Example: Liaison / Certification.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detected_at_assignment_id", nullable = false)
    private ProjectMilestoneAssignment detectedAtAssignment;

    /*
     * Milestone responsible for mistake.
     * Example: Filing / Technical.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_assignment_id", nullable = false)
    private ProjectMilestoneAssignment responsibleAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_department_id")
    private Department requesterDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_department_id")
    private Department responsibleDepartment;

    /*
     * User who raised reopen request.
     * Example: Liaison user / CRT user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    /*
     * First approval manager.
     * Example: Liaison Manager / CRT Manager.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_manager_id", nullable = false)
    private User requesterManager;

    /*
     * Second approval manager.
     * Example: Technical Manager.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_manager_id", nullable = false)
    private User responsibleManager;

    @Column(name = "request_reason", nullable = false, length = 2000)
    private String requestReason;

    @Column(name = "requester_manager_remarks", length = 2000)
    private String requesterManagerRemarks;

    @Column(name = "responsible_manager_remarks", length = 2000)
    private String responsibleManagerRemarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 80)
    private ProjectReopenRequestStatus status =
            ProjectReopenRequestStatus.PENDING_REQUESTER_MANAGER_APPROVAL;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "requested_at", nullable = false)
    private Date requestedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "requester_manager_action_at")
    private Date requesterManagerActionAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "responsible_manager_action_at")
    private Date responsibleManagerActionAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    private Date updatedDate;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}