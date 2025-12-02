package com.doc.dto.project.projectHistory;


import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ProjectHistoryResponseDto {

    private Long projectId;
    private String projectName;
    private Date createdDate;
    private Long createdBy;
    private String createdByName;
    private List<MilestoneHistoryDto> milestones;

}