package com.doc.repository;

public interface TeamWorkloadProjection {
    Long getDepartmentId();

    String getDepartmentName();

    Long getAssignedCount();

    Long getCompletedCount();
}
