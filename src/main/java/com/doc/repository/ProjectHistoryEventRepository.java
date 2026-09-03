package com.doc.repository;

import com.doc.entity.project.ProjectHistoryEvent;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectHistoryEventRepository
        extends JpaRepository<ProjectHistoryEvent, Long> {

    @EntityGraph(attributePaths = {
            "project",
            "milestoneAssignment",
            "milestoneAssignment.milestone"
    })
    @Query("""
            SELECT history
            FROM ProjectHistoryEvent history
            WHERE history.project.id = :projectId
              AND history.isDeleted = false
            ORDER BY history.occurredAt DESC, history.id DESC
            """)
    List<ProjectHistoryEvent> findProjectTimeline(
            @Param("projectId") Long projectId
    );
}
