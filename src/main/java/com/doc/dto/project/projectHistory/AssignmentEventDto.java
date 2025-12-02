package com.doc.dto.project.projectHistory;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class AssignmentEventDto {

    private Date date;
    private Long assignedTo;
    private String assignedToName;
    private Long assignedBy;
    private String assignedByName;
    private String reason;

}