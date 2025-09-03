package com.doc.repository;

import com.doc.entity.project.MilestoneStatus;
import com.doc.entity.project.ProjectMilestoneAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing ProjectMilestoneAssignment entities.
 */
@Repository
public interface ProjectMilestoneAssignmentRepository extends JpaRepository<ProjectMilestoneAssignment, Long> {

    /**
     * Finds all non-deleted ProjectMilestoneAssignment entities for a given project ID.
     *
     * @param projectId The ID of the project.
     * @return A list of non-deleted ProjectMilestoneAssignment entities.
     */
    @Query("SELECT a FROM ProjectMilestoneAssignment a WHERE a.project.id = :projectId AND a.isDeleted = false")
    List<ProjectMilestoneAssignment> findByProjectIdAndIsDeletedFalse(Long projectId);

    Optional<ProjectMilestoneAssignment> findByIdAndIsDeletedFalse(Long assignmentId);

    Page<ProjectMilestoneAssignment> findAllByIsDeletedFalse(Pageable pageable);

    Page<ProjectMilestoneAssignment> findByAssignedUserIdInAndIsVisibleTrueAndStatusIn(
            List<Long> userIds, List<MilestoneStatus> statuses, Pageable pageable);

    Page<ProjectMilestoneAssignment> findByAssignedUserIdAndIsVisibleTrueAndStatusIn(
            Long userId, List<MilestoneStatus> statuses, Pageable pageable);
}