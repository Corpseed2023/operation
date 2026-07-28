package com.doc.repository;

import com.doc.entity.project.ProjectPriority;

import java.time.LocalDate;

public interface DueRiskQueueProjection {
    Long getProjectId();

    String getCompanyName();

    String getProjectNumber();

    Long getMilestoneId();

    String getMilestoneName();

    LocalDate getDueDate();

    Long getOwnerId();

    String getOwnerName();

    ProjectPriority getPriority();
}
