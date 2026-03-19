package com.doc.repository.projectRepo.activity;


import com.doc.entity.project.ProjectActivity;
import com.doc.em.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface ProjectActivityRepository extends JpaRepository<ProjectActivity, Long> {

    Page<ProjectActivity> findByProjectIdAndDeletedFalseOrderByActivityDateDesc(
            Long projectId,
            Pageable pageable
    );

    Page<ProjectActivity> findByProjectIdAndActivityTypeAndDeletedFalseOrderByActivityDateDesc(
            Long projectId,
            ActivityType type,
            Pageable pageable
    );

    Page<ProjectActivity> findByProjectIdAndActivityDateBetweenAndDeletedFalseOrderByActivityDateDesc(
            Long projectId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    @Query("""
        SELECT a FROM ProjectActivity a
        JOIN ProjectComment c ON c.activity.id = a.id
        WHERE a.project.id = :projectId
        AND a.activityType = :type
        AND c.parentCommentId IS NULL
        AND a.deleted = false
        ORDER BY a.activityDate DESC
        """)
    Page<ProjectActivity> findParentCommentActivities(Long projectId, ActivityType type, Pageable pageable);
}