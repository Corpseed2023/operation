package com.doc.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentAutoConfigDto {
    private Long departmentId;
    private boolean autoAssignmentEnabled;
    private boolean availabilityCheckEnabled;
    private boolean ratingPrioritizationEnabled;
}