package com.doc.dto.project.projectHistory;

import com.doc.em.ProjectHistoryActorType;
import com.doc.em.ProjectHistoryEventType;
import com.doc.em.ProjectHistoryReferenceType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProjectHistoryEventResponseDto {

    private Long id;

    private Long projectId;
    private String projectNo;
    private String projectName;

    private Long milestoneAssignmentId;
    private Long milestoneId;
    private String milestoneName;

    private ProjectHistoryEventType eventType;
    private ProjectHistoryReferenceType referenceType;
    private Long referenceId;

    private String eventTitle;
    private String description;
    private String reason;
    private String previousValue;
    private String newValue;

    private ProjectHistoryActorType actorType;
    private Long performedByUserId;
    private String performedByName;

    private Long triggeredByUserId;
    private String triggeredByName;

    private Long previousAssigneeId;
    private String previousAssigneeName;
    private Long newAssigneeId;
    private String newAssigneeName;

    private LocalDateTime occurredAt;
}
