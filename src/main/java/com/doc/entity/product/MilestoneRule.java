package com.doc.entity.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MilestoneRule {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    private Milestone milestone;

    @Column(name = "tat_hours", nullable = false)
    private int tatHours; // SLA deadline in hours

    @Column(name = "strict_approval", nullable = false)
    private boolean strictApproval; // true if approval is mandatory

    @Column(name = "allow_rollback", nullable = false)
    private boolean allowRollback;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts; // max retry for disapproval

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    private Date createdDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    private Date updatedDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // To enable/disable rules


}