package com.doc.dto.procurement;

import com.doc.entity.project.ProcurementStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ProcurementAssignmentResponseDto {

    private Long procurementAssignmentId;

    private Long projectId;
    private String projectName;
    private String projectNo;

    private Long productId;
    private String productName;

    private Long milestoneId;
    private String milestoneName;

    private Long assignedToUserId;
    private String assignedToUserName;

    private Long selectedVendorId;
    private String selectedVendorName;

    private ProcurementStatus status;

    private boolean vendorAvailable;
    private String actionRequired;
    private String message;

    private List<VendorSummaryDto> eligibleVendors;

    private BigDecimal estimatedAmount;
    private BigDecimal finalAmount;

    private Date vendorShortlistedDate;
    private Date poCreatedDate;
    private Date poReleasedDate;

    private Date createdDate;
    private Date updatedDate;
}