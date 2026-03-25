package com.doc.dto.project.report;


import com.doc.dto.project.activity.ProjectActivityResponseDto;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@Data
public class ProjectActivitySummaryDto {

    private String projectStatus;

    private List<MilestoneSummaryDto> milestones;

    private List<ProjectActivityResponseDto> notes;
    private List<ProjectActivityResponseDto> comments;
}

