package com.doc.entity.project;

import com.doc.entity.milestone.Milestone;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.user.User;
import com.doc.entity.vendor.Vendor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "procurement_milestone_assignment")
@Getter
@Setter
@NoArgsConstructor
public class ProcurementMilestoneAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_milestone_map_id")
    private ProductMilestoneMap productMilestoneMap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_vendor_id")
    @Comment("Vendor finalized by procurement team")
    private Vendor selectedVendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcurementStatus status = ProcurementStatus.DRAFT;

    @Temporal(TemporalType.TIMESTAMP)
    private Date vendorShortlistedDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date poCreatedDate;

    private Long createdBy;
    private Long updatedBy;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    private Date poReleasedDate;

}