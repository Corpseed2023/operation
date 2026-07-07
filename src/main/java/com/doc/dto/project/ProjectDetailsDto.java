package com.doc.dto.project;

import com.doc.dto.contact.ContactDetailsDto;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class ProjectDetailsDto {

    private Long id;
    private String name;
    private String projectNo;
    private String priority;

    private LocalDate date;

    private Long productId;
    private String productName;

    // Sales Person
    private Long salesPersonId;
    private String salesPersonName;

    private Long companyId;
    private String companyName;
    private String rating;

    private Long companyUnitId;
    private String companyUnitName;

    private Long contactId;
    private String contactName;

    private List<ContactDetailsDto> contacts;

    private Date createdDate;
    private Date updatedDate;

    private Long applicantId;
    private String applicantName;

    // NEW: ProcurementMilestoneAssignment ID (null if no procurement assignment exists for this project)
    // This field tells the frontend whether this project has an active Procurement milestone assignment
    // and provides the ID needed for further procurement-related API calls.
    private Long procurementMilestoneAssignmentId;

}