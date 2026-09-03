package com.doc.repository.projection;

public interface TeamWorkloadProjection {
    Long getDepartmentId();
    String getDepartmentName();
    Long getMilestoneId();
    String getMilestoneName();
    Long getAssignedCount();
    Long getCompletedCount();
}