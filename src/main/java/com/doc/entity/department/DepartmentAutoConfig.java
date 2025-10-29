package com.doc.entity.department;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Entity
@Table(name = "department_auto_config")
@Getter
@Setter
@NoArgsConstructor
public class DepartmentAutoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Auto-config ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @Comment("Associated department")
    private Department department;

    @Column(name = "auto_assignment_enabled", nullable = false)
    @Comment("Whether auto-assignment is enabled")
    private boolean autoAssignmentEnabled = false;

    @Column(name = "availability_required", nullable = false)
    @Comment("Whether availability check is required")
    private boolean availabilityRequired = true;

    @Column(name = "rating_prioritization_enabled", nullable = false)
    @Comment("Whether rating prioritization is enabled")
    private boolean ratingPrioritizationEnabled = false;

    @Column(name = "company_alignment_enabled", nullable = false)
    @Comment("Whether company alignment is enabled")
    private boolean companyAlignmentEnabled = false;

    @Column(name = "manual_only", nullable = false)
    @Comment("Whether department requires manual assignment only")
    private boolean manualOnly = false;

    @Column(name = "round_robin_enabled", nullable = false)
    @Comment("Whether round-robin selection is enabled")
    private boolean roundRobinEnabled = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    @Comment("Creation date")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    @Comment("Update date")
    private Date updatedDate;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;
}
