package com.doc.repository;

public interface MilestoneOverviewProjection {

    Long getMilestoneId();

    String getMilestoneName();

    Long getTotalProjects();

    Long getCompletedProjects();
}
