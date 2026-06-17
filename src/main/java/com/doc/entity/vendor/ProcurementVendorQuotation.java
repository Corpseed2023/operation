package com.doc.entity.vendor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "procurement_vendor_quotations")
@Getter
@Setter
@NoArgsConstructor
public class ProcurementVendorQuotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procurement_assignment_id", nullable = false)
    private ProcurementMilestoneAssignment procurementAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal quotedAmount;

    @Column(length = 1000)
    private String remarks;

    @Column(length = 1000)
    private String quotationFilePath;

    private boolean isSelected = false;

    private Long createdBy;
    private Long updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private boolean isDeleted = false;
}