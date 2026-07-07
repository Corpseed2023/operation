package com.doc.dto.project.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectOverviewCardDto {

    private String type;          // IN_PROGRESS, AWAITING_DOCUMENTS, DELAYED

    private String label;         // In Progress, Awaiting Documents, Delayed

    private String description;   // Currently being worked on, Waiting for docs/info

    private Long count;

    private Integer percentage;
}