package com.doc.service;

import com.doc.dto.project.report.ProjectActivitySummaryDto;

public interface ActivityService {
    ProjectActivitySummaryDto getProjectSummary(Long projectId);
}
