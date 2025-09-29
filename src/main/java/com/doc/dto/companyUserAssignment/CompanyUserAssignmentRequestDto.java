package com.doc.dto.companyUserAssignment;

import lombok.Data;

@Data
public class CompanyUserAssignmentRequestDto {
    private Long companyId;
    private Long departmentId;
    private Long primaryUserId;
    private Long alternativeUserId;
    private Long createdBy;
    private Long updatedBy;
}