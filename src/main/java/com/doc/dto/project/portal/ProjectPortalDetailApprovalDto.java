package com.doc.dto.project.portal;

import lombok.Data;

@Data
public class ProjectPortalDetailApprovalDto {
    private String status; // APPROVED or REJECTED
    private String approvalRemarks;
}