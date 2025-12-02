package com.doc.dto.project.projectHistory;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class StatusChangeEventDto {

    private Date date;
    private String previousStatus;
    private String newStatus;
    private Long changedBy;
    private String changedByName;
    private String reason;

}