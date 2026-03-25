package com.doc.dto.project.report;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MilestoneSummaryDto {

    private String milestoneName;

    private Long assigneeUserId;
    private String assigneeUserName;

    private String status;
}