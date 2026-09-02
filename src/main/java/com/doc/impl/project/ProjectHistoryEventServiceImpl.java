package com.doc.impl.project;

import com.doc.dto.project.projectHistory.ProjectHistoryEventResponseDto;
import com.doc.em.ProjectHistoryEventType;
import com.doc.em.ProjectHistoryReferenceType;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectHistoryEvent;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.ProjectHistoryEventRepository;
import com.doc.repository.ProjectRepository;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.service.project.ProjectHistoryEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ProjectHistoryEventServiceImpl
        implements ProjectHistoryEventService {

    private final ProjectRepository projectRepository;
    private final ProjectHistoryEventRepository historyRepository;
    private final ProjectMilestoneAssignmentRepository milestoneAssignmentRepository;

    public ProjectHistoryEventServiceImpl(
            ProjectRepository projectRepository,
            ProjectHistoryEventRepository historyRepository,
            ProjectMilestoneAssignmentRepository milestoneAssignmentRepository
    ) {
        this.projectRepository = projectRepository;
        this.historyRepository = historyRepository;
        this.milestoneAssignmentRepository = milestoneAssignmentRepository;
    }

    @Override
    @Transactional
    public void saveHistory(
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
    ) {

        Project project = projectRepository.findById(projectId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));

        ProjectMilestoneAssignment milestoneAssignment = null;

        if (milestoneAssignmentId != null) {
            milestoneAssignment = milestoneAssignmentRepository
                    .findById(milestoneAssignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Milestone assignment not found",
                            "ERR_MILESTONE_ASSIGNMENT_NOT_FOUND"
                    ));
        }

        ProjectHistoryEvent event = new ProjectHistoryEvent();

        event.setProject(project);
        event.setMilestoneAssignment(milestoneAssignment);

        /*
         * Snapshot milestone name so even if milestone name changes later,
         * old history still shows the original value.
         */
        if (milestoneAssignment != null
                && milestoneAssignment.getMilestone() != null) {

            event.setMilestoneName(
                    milestoneAssignment.getMilestone().getName()
            );
        }

        event.setEventType(eventType);

        event.setReferenceType(referenceType);
        event.setReferenceId(referenceId);

        event.setEventTitle(eventTitle);
        event.setDescription(description);
        event.setReason(reason);

        event.setPreviousValue(previousValue);
        event.setNewValue(newValue);

        event.setPerformedByUserId(performedByUserId);
        event.setPerformedByName(performedByName);

        event.setOccurredAt(LocalDateTime.now());

        historyRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectHistoryEventResponseDto> getProjectTimeline(
            Long projectId,
            ProjectHistoryEventType eventType,
            Long milestoneAssignmentId,
            Pageable pageable
    ) {

        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException(
                    "projectId must be greater than zero"
            );
        }

        if (milestoneAssignmentId != null
                && milestoneAssignmentId <= 0) {
            throw new IllegalArgumentException(
                    "milestoneAssignmentId must be greater than zero"
            );
        }

        projectRepository.findById(projectId)
                .filter(project -> !project.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));

        int requestedPage = pageable != null
                ? Math.max(pageable.getPageNumber(), 0)
                : 0;

        int requestedSize = pageable != null
                ? pageable.getPageSize()
                : 20;

        int safeSize = Math.min(
                Math.max(requestedSize, 1),
                100
        );

        Pageable safePageable = PageRequest.of(
                requestedPage,
                safeSize
        );

        return historyRepository.findProjectTimeline(
                        projectId,
                        eventType,
                        milestoneAssignmentId,
                        safePageable
                )
                .map(this::mapToResponse);
    }

    private ProjectHistoryEventResponseDto mapToResponse(
            ProjectHistoryEvent event
    ) {

        ProjectHistoryEventResponseDto dto =
                new ProjectHistoryEventResponseDto();

        dto.setId(event.getId());

        if (event.getProject() != null) {
            dto.setProjectId(event.getProject().getId());
            dto.setProjectNo(event.getProject().getProjectNo());
            dto.setProjectName(event.getProject().getName());
        }

        ProjectMilestoneAssignment assignment =
                event.getMilestoneAssignment();

        if (assignment != null) {

            dto.setMilestoneAssignmentId(assignment.getId());

            if (assignment.getMilestone() != null) {
                dto.setMilestoneId(
                        assignment.getMilestone().getId()
                );
            }
        }

        dto.setMilestoneName(event.getMilestoneName());

        dto.setEventType(event.getEventType());

        dto.setReferenceType(event.getReferenceType());
        dto.setReferenceId(event.getReferenceId());

        dto.setEventTitle(event.getEventTitle());
        dto.setDescription(event.getDescription());
        dto.setReason(event.getReason());

        dto.setPreviousValue(event.getPreviousValue());
        dto.setNewValue(event.getNewValue());

        dto.setActorType(event.getActorType());

        dto.setPerformedByUserId(
                event.getPerformedByUserId()
        );

        dto.setPerformedByName(
                event.getPerformedByName()
        );

        dto.setTriggeredByUserId(
                event.getTriggeredByUserId()
        );

        dto.setTriggeredByName(
                event.getTriggeredByName()
        );

        dto.setPreviousAssigneeId(
                event.getPreviousAssigneeId()
        );

        dto.setPreviousAssigneeName(
                event.getPreviousAssigneeName()
        );

        dto.setNewAssigneeId(
                event.getNewAssigneeId()
        );

        dto.setNewAssigneeName(
                event.getNewAssigneeName()
        );

        dto.setOccurredAt(event.getOccurredAt());

        return dto;
    }
}