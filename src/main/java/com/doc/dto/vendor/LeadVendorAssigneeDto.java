package com.doc.dto.vendor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeadVendorAssigneeDto {

    private boolean found;

    private Long vendorRequestId;

    private Long leadId;

    private Long assignedUserId;

    private String assignedUserName;

    private String assignedUserEmail;
}