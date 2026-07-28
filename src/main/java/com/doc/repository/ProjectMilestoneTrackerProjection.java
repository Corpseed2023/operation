package com.doc.repository;

import java.time.LocalDate;

public interface ProjectMilestoneTrackerProjection {

    Long getAssignmentId();

    Long getProjectId();

    Long getMilestoneId();

    String getMilestoneName();

    Integer getDisplayOrder();

    Long getStatusId();

    String getStatusName();

    Integer getProgressPercentage();

    Long getAssignedUserId();

    String getAssignedUserName();

    LocalDate getDueDate();
}
