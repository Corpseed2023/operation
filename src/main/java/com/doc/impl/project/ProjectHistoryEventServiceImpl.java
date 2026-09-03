package com.doc.impl.project;

import com.doc.dto.project.projectHistory.ProjectHistoryEventResponseDto;
import com.doc.em.ProjectHistoryEventType;
import com.doc.em.ProjectHistoryReferenceType;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectHistoryEvent;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.ProjectHistoryEventRepository;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.repository.ProjectRepository;
import com.doc.service.project.ProjectHistoryEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

        Project project = projectRepository
                .findByIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));

        ProjectMilestoneAssignment milestoneAssignment = null;

        if (milestoneAssignmentId != null) {

            milestoneAssignment = milestoneAssignmentRepository
                    .findByIdAndProjectIdAndIsDeletedFalse(
                            milestoneAssignmentId,
                            projectId
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Milestone assignment not found for project",
                            "ERR_MILESTONE_ASSIGNMENT_NOT_FOUND"
                    ));
        }

        ProjectHistoryEvent event = new ProjectHistoryEvent();

        event.setProject(project);
        event.setMilestoneAssignment(milestoneAssignment);

        /*
         * Keep milestone name as snapshot.
         *
         * Even if milestone master name changes in future,
         * old project history remains unchanged.
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
    public List<ProjectHistoryEventResponseDto> getProjectTimeline(
            Long projectId
    ) {

        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException(
                    "Project ID must be greater than zero"
            );
        }

        /*
         * Validate project first.
         */
        projectRepository
                .findByIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));

        /*
         * Fetch complete history of project.
         *
         * No event type filter.
         * No milestone filter.
         * No pagination.
         */
        return historyRepository
                .findProjectTimeline(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ProjectHistoryEventResponseDto mapToResponse(
            ProjectHistoryEvent event
    ) {

        ProjectHistoryEventResponseDto dto =
                new ProjectHistoryEventResponseDto();

        dto.setId(event.getId());

        /*
         * Project information
         */
        if (event.getProject() != null) {

            dto.setProjectId(
                    event.getProject().getId()
            );

            dto.setProjectNo(
                    event.getProject().getProjectNo()
            );

            dto.setProjectName(
                    event.getProject().getName()
            );
        }

        /*
         * Milestone information
         */
        ProjectMilestoneAssignment assignment =
                event.getMilestoneAssignment();

        if (assignment != null) {

            dto.setMilestoneAssignmentId(
                    assignment.getId()
            );

            if (assignment.getMilestone() != null) {

                dto.setMilestoneId(
                        assignment.getMilestone().getId()
                );
            }
        }

        /*
         * Snapshot milestone name
         */
        dto.setMilestoneName(
                event.getMilestoneName()
        );

        /*
         * Event information
         */
        dto.setEventType(
                event.getEventType()
        );

        dto.setReferenceType(
                event.getReferenceType()
        );

        dto.setReferenceId(
                event.getReferenceId()
        );

        dto.setEventTitle(
                event.getEventTitle()
        );

        dto.setDescription(
                event.getDescription()
        );

        dto.setReason(
                event.getReason()
        );

        /*
         * Before / after value
         */
        dto.setPreviousValue(
                event.getPreviousValue()
        );

        dto.setNewValue(
                event.getNewValue()
        );

        /*
         * Actor
         */
        dto.setActorType(
                event.getActorType()
        );

        dto.setPerformedByUserId(
                event.getPerformedByUserId()
        );

        dto.setPerformedByName(
                event.getPerformedByName()
        );

        /*
         * Triggered by
         */
        dto.setTriggeredByUserId(
                event.getTriggeredByUserId()
        );

        dto.setTriggeredByName(
                event.getTriggeredByName()
        );

        /*
         * Assignment history
         */
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

        /*
         * Event timestamp
         */
        dto.setOccurredAt(
                event.getOccurredAt()
        );

        return dto;
    }
}