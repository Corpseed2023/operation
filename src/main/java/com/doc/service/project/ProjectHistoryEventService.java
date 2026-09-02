package com.doc.service.project;

import com.doc.dto.project.projectHistory.ProjectHistoryEventResponseDto;
import com.doc.em.ProjectHistoryEventType;
import com.doc.em.ProjectHistoryReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectHistoryEventService {

    Page<ProjectHistoryEventResponseDto> getProjectTimeline(
            Long projectId,
            ProjectHistoryEventType eventType,
            Long milestoneAssignmentId,
            Pageable pageable
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