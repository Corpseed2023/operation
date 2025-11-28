package com.doc.dto.project.portal;

import lombok.Data;
import java.util.List;

@Data
public class ProjectPortalDetailListResponseDto {
    private Long projectId;
    private String projectNo;
    private String companyName;
    private List<ProjectPortalDetailResponseDto> portals;
}