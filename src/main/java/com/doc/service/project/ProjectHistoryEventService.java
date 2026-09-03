package com.doc.service.project;

import com.doc.dto.project.projectHistory.ProjectHistoryEventResponseDto;
import com.doc.em.ProjectHistoryEventType;
import com.doc.em.ProjectHistoryReferenceType;

import java.util.List;

public interface ProjectHistoryEventService {

    List<ProjectHistoryEventResponseDto> getProjectTimeline(
            Long projectId
    );

    void saveHistory(
            Long projectId,
            Long milestoneAssignmentId,
            ProjectHistoryEventType eventType,
            ProjectHistoryReferenceType referenceType,
            Long referenceId,
            String eventTitle,
            String description,
            String reason,
            String previousValue,
            String newValue,
            Long performedByUserId,
            String performedByName
    );
}