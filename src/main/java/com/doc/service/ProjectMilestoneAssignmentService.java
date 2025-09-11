package com.doc.service;


import com.doc.dto.ProjectMilestoneassignment.ReassignMilestoneDto;
import com.doc.dto.ProjectMilestoneassignment.UpdateMilestoneStatusDto;

public interface ProjectMilestoneAssignmentService {
    void updateMilestoneStatus(UpdateMilestoneStatusDto updateDto);
    void reassignMilestone(ReassignMilestoneDto reassignDto);
}