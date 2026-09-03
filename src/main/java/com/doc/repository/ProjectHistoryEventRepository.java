package com.doc.repository;

import com.doc.em.ProjectHistoryEventType;
import com.doc.entity.project.ProjectHistoryEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectHistoryEventRepository
        extends JpaRepository<ProjectHistoryEvent, Long> {

    @EntityGraph(attributePaths = {
            "project",
            "milestoneAssignment",
            "milestoneAssignment.milestone"
    })
    @Query(
            value = """
                    SELECT history
                    FROM ProjectHistoryEvent history
                    WHERE history.project.id = :projectId
                      AND history.isDeleted = false
                      AND (
                            :eventType IS NULL
                            OR history.eventType = :eventType
                      )
                      AND (
                            :milestoneAssignmentId IS NULL
                            OR history.milestoneAssignment.id = :milestoneAssignmentId
                      )
                    ORDER BY history.occurredAt DESC, history.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(history.id)
                    FROM ProjectHistoryEvent history
                    WHERE history.project.id = :projectId
                      AND history.isDeleted = false
                      AND (
                            :eventType IS NULL
                            OR history.eventType = :eventType
                      )
                      AND (
                            :milestoneAssignmentId IS NULL
                            OR history.milestoneAssignment.id = :milestoneAssignmentId
                      )
                    """
    )
    Page<ProjectHistoryEvent> findProjectTimeline(
            @Param("projectId") Long projectId,
            @Param("eventType") ProjectHistoryEventType eventType,
            @Param("milestoneAssignmentId") Long milestoneAssignmentId,
            Pageable pageable
    );
}
