package com.doc.dto.auto;

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
    private boolean roundRobinEnabled ;

    /** NEW – shows the user exactly which features are active */
    private String enabledFeatures;          // e.g. "Round-Robin, Availability Check"
}