package com.doc.repository;

import com.doc.entity.milestone.MilestoneStatus;
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
     * Ordered by product milestone step order.
     */
    @Query("""
            SELECT a
            FROM ProjectMilestoneAssignment a
            JOIN a.productMilestoneMap pmm
            WHERE a.project.id = :projectId
              AND a.isDeleted = false
            ORDER BY pmm.order ASC, a.id ASC
            """)
    List<ProjectMilestoneAssignment> findByProjectIdAndIsDeletedFalse(
            @Param("projectId") Long projectId
    );

    /**
     * Finds a non-deleted ProjectMilestoneAssignment by its ID.
     */
    @Query("""
            SELECT a
            FROM ProjectMilestoneAssignment a
            WHERE a.id = :assignmentId
              AND a.isDeleted = false
            """)
    Optional<ProjectMilestoneAssignment> findActiveUserById(
            @Param("assignmentId") Long assignmentId
    );

    /**
     * Finds all non-deleted ProjectMilestoneAssignment entities with pagination.
     * Kept old behavior unchanged.
     */
    Page<ProjectMilestoneAssignment> findAllByIsDeletedFalse(Pageable pageable);

    /**
     * Finds non-deleted ProjectMilestoneAssignment entities for a list of assigned user IDs,
     * where milestones are visible and in specified statuses, with pagination.
     * Kept old behavior unchanged to avoid pagination/distribution impact.
     */
    @Query("""
            SELECT a
            FROM ProjectMilestoneAssignment a
            WHERE a.assignedUser.id IN :userIds
              AND a.isVisible = true
              AND a.status IN :statuses
              AND a.isDeleted = false
            """)
    Page<ProjectMilestoneAssignment> findByAssignedUserIdInAndIsVisibleTrueAndStatusIn(
            @Param("userIds") List<Long> userIds,
            @Param("statuses") List<MilestoneStatus> statuses,
            Pageable pageable
    );

    /**
     * Finds non-deleted ProjectMilestoneAssignment entities for a specific assigned user ID,
     * where milestones are visible and in specified statuses, with pagination.
     * Kept old behavior unchanged to avoid pagination/distribution impact.
     */
    @Query("""
            SELECT a
            FROM ProjectMilestoneAssignment a
            WHERE a.assignedUser.id = :userId
              AND a.isVisible = true
              AND a.status IN :statuses
              AND a.isDeleted = false
            """)
    Page<ProjectMilestoneAssignment> findByAssignedUserIdAndIsVisibleTrueAndStatusIn(
            @Param("userId") Long userId,
            @Param("statuses") List<MilestoneStatus> statuses,
            Pageable pageable
    );

    /**
     * Finds a non-deleted ProjectMilestoneAssignment for a specific project ID and assigned user ID.
     */
    @Query("""
            SELECT a
            FROM ProjectMilestoneAssignment a
            WHERE a.project.id = :projectId
              AND a.assignedUser.id = :userId
              AND a.isDeleted = false
            """)
    Optional<ProjectMilestoneAssignment> findByProjectIdAndAssignedUserIdAndIsDeletedFalse(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId
    );

    /**
     * Finds a non-deleted ProjectMilestoneAssignment for a specific project ID and milestone ID.
     */
    @Query("""
            SELECT a
            FROM ProjectMilestoneAssignment a
            WHERE a.project.id = :projectId
              AND a.milestone.id = :milestoneId
              AND a.isDeleted = false
            """)
    Optional<ProjectMilestoneAssignment> findByProjectIdAndMilestoneIdAndIsDeletedFalse(
            @Param("projectId") Long projectId,
            @Param("milestoneId") Long milestoneId
    );

    /**
     * For Managers: Get all VISIBLE milestones, including COMPLETED,
     * assigned to any team member in a specific project.
     * Ordered by product milestone step order.
     */
    @Query("""
            SELECT a
            FROM ProjectMilestoneAssignment a
            JOIN a.productMilestoneMap pmm
            WHERE a.project.id = :projectId
              AND a.assignedUser.id IN :userIds
              AND a.isVisible = true
              AND a.isDeleted = false
            ORDER BY pmm.order ASC, a.id ASC
            """)
    List<ProjectMilestoneAssignment> findByProjectIdAndAssignedUserIdInAndIsVisibleTrue(
            @Param("projectId") Long projectId,
            @Param("userIds") List<Long> userIds
    );

    /**
     * For Regular Users: Get all VISIBLE milestones, including COMPLETED,
     * assigned to this specific user in a specific project.
     * Ordered by product milestone step order.
     */
    @Query("""
            SELECT a
            FROM ProjectMilestoneAssignment a
            JOIN a.productMilestoneMap pmm
            WHERE a.project.id = :projectId
              AND a.assignedUser.id = :userId
              AND a.isVisible = true
              AND a.isDeleted = false
            ORDER BY pmm.order ASC, a.id ASC
            """)
    List<ProjectMilestoneAssignment> findByProjectIdAndAssignedUserIdAndIsVisibleTrueAndIsDeletedFalse(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId
    );

    /**
     * Finds a non-deleted assignment by assignment ID and project ID.
     */
    @Query("""
            SELECT a
            FROM ProjectMilestoneAssignment a
            WHERE a.id = :id
              AND a.project.id = :projectId
              AND a.isDeleted = false
            """)
    Optional<ProjectMilestoneAssignment> findByIdAndProjectIdAndIsDeletedFalse(
            @Param("id") Long id,
            @Param("projectId") Long projectId
    );

    /**
     * Total milestones of a project.
     */
    long countByProject_IdAndIsDeletedFalse(Long projectId);

    /**
     * Completed milestones of a project.
     */
    long countByProject_IdAndStatus_NameAndIsDeletedFalse(Long projectId, String statusName);

    boolean existsByProductMilestoneMapId(Long productMilestoneMapId);



}