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
    @Comment("Primary key: Config ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @Comment("Associated department (e.g., CRT, Legal)")
    private Department department;

    @Column(name = "auto_assignment_enabled", nullable = false)
    @Comment("Toggle for auto-assignment in this department")
    private boolean autoAssignmentEnabled = false;

    @Column(name = "availability_check_enabled", nullable = false)
    @Comment("Toggle for availability check (login status)")
    private boolean availabilityCheckEnabled = false;

    @Column(name = "rating_prioritization_enabled", nullable = false)
    @Comment("Toggle for rating-based prioritization per product/service")
    private boolean ratingPrioritizationEnabled = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    @Comment("Creation date")
    private Date createdDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Update date")
    private Date updatedDate;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;
}