package com.doc.repository;

import java.time.LocalDateTime;

public interface UserMilestonePerformanceProjection {

    //Long getAssignmentId();

    Long getUserId();

    String getUserName();

    Long getProjectId();

    String getProjectNumber();

    String getProjectName();

    Long getProductId();

    String getProductName();

    Long getMilestoneId();

    String getMilestoneName();

    Double getPerformanceTatHours();

    LocalDateTime getStartedDate();

    LocalDateTime getCompletedDate();

    Long getStatusId();

    Boolean getPerformanceTatApplicable();

}