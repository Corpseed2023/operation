package com.doc.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentAutoConfigDto {

    private Long departmentId;
    private String departmentName;
    private boolean autoAssignmentEnabled;
    private boolean availabilityRequired;
    private boolean ratingPrioritizationEnabled;
    private boolean companyAlignmentEnabled;
    private boolean manualOnly;
    private boolean roundRobinEnabled = true; // NEW: Toggle round-robin
}