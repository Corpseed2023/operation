package com.doc.service.project;

import com.doc.dto.ProjectMilestoneassignment.*;

import java.util.List;

public interface ProjectMilestoneAssignmentService {
    void updateMilestoneStatus(UpdateMilestoneStatusDto updateDto);
    ReassignMilestoneResponseDto reassignMilestone(ReassignMilestoneDto reassignDto);

    void sendBackToPreviousMilestone(SendBackToPreviousMilestoneDto dto);

    List<MilestoneAcknowledgementResponseDto>
    getPreviousCompletionAcknowledgements(
            Long currentAssignmentId,
            Long userId
    );


}