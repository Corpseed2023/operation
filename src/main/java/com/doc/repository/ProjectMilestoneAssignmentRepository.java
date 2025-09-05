package com.doc.repository;

import com.doc.entity.project.MilestoneStatus;
import com.doc.entity.project.ProjectMilestoneAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<ProjectMilestoneAssignment> findByProjectIdAndIsDeletedFalse(@Param("projectId") Long projectId);

    /**
     * Finds a non-deleted ProjectMilestoneAssignment by its ID.
     *
     * @param assignmentId The ID of the milestone assignment.
     * @return An Optional containing the ProjectMilestoneAssignment if found, or empty if not found or deleted.
     */
    Optional<ProjectMilestoneAssignment> findByIdAndIsDeletedFalse(@Param("assignmentId") Long assignmentId);

    /**
     * Finds all non-deleted ProjectMilestoneAssignment entities with pagination.
     *
     * @param pageable Pagination information.
     * @return A Page of non-deleted ProjectMilestoneAssignment entities.
     */
    Page<ProjectMilestoneAssignment> findAllByIsDeletedFalse(Pageable pageable);

    /**
     * Finds non-deleted ProjectMilestoneAssignment entities for a list of assigned user IDs,
     * where milestones are visible and in specified statuses, with pagination.
     *
     * @param userIds  List of user IDs assigned to the milestones.
     * @param statuses List of milestone statuses to filter (e.g., NEW, IN_PROGRESS).
     * @param pageable Pagination information.
     * @return A Page of ProjectMilestoneAssignment entities.
     */
    @Query("SELECT a FROM ProjectMilestoneAssignment a WHERE a.assignedUser.id IN :userIds " +
            "AND a.isVisible = true AND a.status IN :statuses AND a.isDeleted = false")
    Page<ProjectMilestoneAssignment> findByAssignedUserIdInAndIsVisibleTrueAndStatusIn(
            @Param("userIds") List<Long> userIds,
            @Param("statuses") List<MilestoneStatus> statuses,
            Pageable pageable);

    /**
     * Finds non-deleted ProjectMilestoneAssignment entities for a specific assigned user ID,
     * where milestones are visible and in specified statuses, with pagination.
     *
     * @param userId   The ID of the assigned user.
     * @param statuses List of milestone statuses to filter (e.g., NEW, IN_PROGRESS).
     * @param pageable Pagination information.
     * @return A Page of ProjectMilestoneAssignment entities.
     */
    @Query("SELECT a FROM ProjectMilestoneAssignment a WHERE a.assignedUser.id = :userId " +
            "AND a.isVisible = true AND a.status IN :statuses AND a.isDeleted = false")
    Page<ProjectMilestoneAssignment> findByAssignedUserIdAndIsVisibleTrueAndStatusIn(
            @Param("userId") Long userId,
            @Param("statuses") List<MilestoneStatus> statuses,
            Pageable pageable);

    /**
     * Finds non-deleted ProjectMilestoneAssignment entities for a specific project ID and a list of assigned user IDs,
     * where milestones are visible and in specified statuses.
     *
     * @param projectId The ID of the project.
     * @param userIds   List of user IDs assigned to the milestones.
     * @param statuses  List of milestone statuses to filter (e.g., NEW, IN_PROGRESS).
     * @return A List of ProjectMilestoneAssignment entities.
     */
    @Query("SELECT a FROM ProjectMilestoneAssignment a WHERE a.project.id = :projectId " +
            "AND a.assignedUser.id IN :userIds AND a.isVisible = true AND a.status IN :statuses AND a.isDeleted = false")
    List<ProjectMilestoneAssignment> findByProjectIdAndAssignedUserIdInAndIsVisibleTrueAndStatusIn(
            @Param("projectId") Long projectId,
            @Param("userIds") List<Long> userIds,
            @Param("statuses") List<MilestoneStatus> statuses);

    /**
     * Finds non-deleted ProjectMilestoneAssignment entities for a specific project ID and assigned user ID,
     * where milestones are visible and in specified statuses.
     *
     * @param projectId The ID of the project.
     * @param userId    The ID of the assigned user.
     * @param statuses  List of milestone statuses to filter (e.g., NEW, IN_PROGRESS).
     * @return A List of ProjectMilestoneAssignment entities.
     */
    @Query("SELECT a FROM ProjectMilestoneAssignment a WHERE a.project.id = :projectId " +
            "AND a.assignedUser.id = :userId AND a.isVisible = true AND a.status IN :statuses AND a.isDeleted = false")
    List<ProjectMilestoneAssignment> findByProjectIdAndAssignedUserIdAndIsVisibleTrueAndStatusIn(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("statuses") List<MilestoneStatus> statuses);
}