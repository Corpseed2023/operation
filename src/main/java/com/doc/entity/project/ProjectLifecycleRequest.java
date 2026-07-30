package com.doc.entity.project;

import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "project_lifecycle_requests",
        indexes = {
                @Index(
                        name = "idx_lifecycle_project",
                        columnList = "project_id"
                ),
                @Index(
                        name = "idx_lifecycle_status",
                        columnList = "request_status"
                ),
                @Index(
                        name = "idx_lifecycle_requested_by",
                        columnList = "requested_by_id"
                )
        }
)
@Getter
@Setter
public class ProjectLifecycleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "project_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_lifecycle_request_project"
            )
    )
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "action_type",
            nullable = false,
            length = 30
    )
    private ProjectLifecycleAction actionType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "request_status",
            nullable = false,
            length = 30
    )
    private ProjectLifecycleRequestStatus requestStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "requested_by_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_lifecycle_requested_by"
            )
    )
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reviewed_by_id",
            foreignKey = @ForeignKey(
                    name = "fk_lifecycle_reviewed_by"
            )
    )
    private User reviewedBy;

    @Column(
            name = "request_reason",
            nullable = false,
            length = 2000
    )
    private String requestReason;

    @Column(
            name = "review_remark",
            length = 2000
    )
    private String reviewRemark;

    /*
     * Project status before request approval.
     * Used for audit/history only.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "previous_project_status_id",
            foreignKey = @ForeignKey(
                    name = "fk_lifecycle_previous_status"
            )
    )
    private ProjectStatus previousProjectStatus;

    @Column(
            name = "requested_at",
            nullable = false
    )
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(
            name = "created_by",
            nullable = false
    )
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(
            name = "created_date",
            nullable = false
    )
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(
            name = "is_deleted",
            nullable = false
    )
    private boolean deleted = false;

    @Version
    @Column(name = "version")
    private Long version;
}