package com.doc.repository.projectRepo.activity;

import com.doc.entity.project.activity.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {

    Optional<ProjectComment> findByActivityId(Long activityId);
}