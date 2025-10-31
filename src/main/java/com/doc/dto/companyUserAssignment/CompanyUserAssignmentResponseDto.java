package com.doc.dto.companyUserAssignment;


import lombok.Data;

import java.util.Date;

@Data
public class CompanyUserAssignmentResponseDto {
    private Long id;
    private Long companyId;
    private Long departmentId;
    private Long primaryUserId;
    private Long alternativeUserId;
    private boolean isDeleted;
    private boolean isActive;
    private Date createdDate;
    private Date updatedDate;
    private Long createdBy;
    private Long updatedBy;
}