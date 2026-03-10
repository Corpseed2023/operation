package com.doc.repository.projectRepo.activity;


import com.doc.entity.project.ProjectActivity;
import com.doc.em.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}