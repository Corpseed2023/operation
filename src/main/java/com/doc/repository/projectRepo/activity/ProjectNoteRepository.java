package com.doc.repository.projectRepo.activity;

import com.doc.entity.project.activity.ProjectNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectNoteRepository extends JpaRepository<ProjectNote, Long> {

    Optional<ProjectNote> findByActivityId(Long activityId);
}