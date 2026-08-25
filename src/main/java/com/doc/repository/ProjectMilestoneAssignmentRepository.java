package com.doc.repository;

import com.doc.entity.milestone.MilestoneStatus;
import com.doc.entity.project.ProjectMilestoneAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    @Query("""
        SELECT DISTINCT a
        FROM ProjectMilestoneAssignment a
        LEFT JOIN FETCH a.project p
        LEFT JOIN FETCH a.productMilestoneMap pmm
        LEFT JOIN FETCH a.milestone m
        LEFT JOIN FETCH m.departments d
        LEFT JOIN FETCH a.assignedUser u
        LEFT JOIN FETCH a.status s
        WHERE p.id IN :projectIds
          AND a.isDeleted = false
        ORDER BY p.id ASC, pmm.order ASC, a.id ASC
        """)
    List<ProjectMilestoneAssignment> findDashboardAssignmentsByProjectIds(
            @Param("projectIds") List<Long> projectIds
    );

    @Query("""
        SELECT
            m.id AS milestoneId,
            m.name AS milestoneName,

            COUNT(DISTINCT p.id) AS totalProjects,

            COUNT(
                DISTINCT CASE
                    WHEN s.id = 3 THEN p.id
                    ELSE NULL
                END
            ) AS completedProjects

        FROM ProjectMilestoneAssignment pma

        JOIN pma.project p
        JOIN pma.milestone m
        JOIN pma.status s

        JOIN pma.assignedUser u
        JOIN u.departments d

        WHERE pma.isDeleted = false
          AND pma.isVisible = true
          AND p.isDeleted = false
          AND pma.assignedUser IS NOT NULL

          AND (
                :departmentId IS NULL
                OR d.id = :departmentId
          )

        GROUP BY
            m.id,
            m.name

        ORDER BY m.id
        """)
    List<MilestoneOverviewProjection> getMilestoneOverview(
            @Param("departmentId") Long departmentId
    );

    @Query("""
        SELECT
            d.id AS departmentId,
            d.name AS departmentName,

            COUNT(pma.id) AS assignedCount,

            COALESCE(
                SUM(
                    CASE
                        WHEN pma.status.id = 3 THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS completedCount

        FROM ProjectMilestoneAssignment pma
        JOIN pma.assignedUser u
        JOIN u.departments d
        JOIN pma.project p

        WHERE pma.isDeleted = false
          AND p.isDeleted = false
          AND pma.assignedUser IS NOT NULL

          AND (
                :departmentId IS NULL
                OR d.id = :departmentId
          )

        GROUP BY
            d.id,
            d.name

        ORDER BY d.name
        """)
    List<TeamWorkloadProjection> getTeamWorkload(
            @Param("departmentId") Long departmentId
    );


    @Query(
            value = """
                SELECT
                    p.id AS projectId,
                    c.name AS companyName,
                    p.project_no AS projectNumber,
                    m.id AS milestoneId,
                    m.name AS milestoneName,
                    pma.date AS dueDate,
                    u.id AS ownerId,
                    u.full_name AS ownerName,
                    p.priority AS priority

                FROM project_milestone_assignment pma

                INNER JOIN project p
                        ON p.id = pma.project_id

                INNER JOIN milestones m
                        ON m.id = pma.milestone_id

                INNER JOIN company c
                        ON c.id = p.company_id

                LEFT JOIN users u
                       ON u.id = pma.assigned_user_id

                LEFT JOIN user_department_map udm
                       ON udm.user_id = pma.assigned_user_id

                WHERE pma.is_deleted = 0
                  AND p.is_deleted = 0
                  AND pma.is_visible = 1
                  AND pma.date IS NOT NULL
                  AND pma.status_id <> 3

                  AND (
                        :departmentId IS NULL
                        OR udm.dept_id = :departmentId
                  )

                  AND pma.date <= DATE_ADD(
                      CURDATE(),
                      INTERVAL :upcomingDays DAY
                  )

                ORDER BY
                    CASE
                        WHEN pma.date < CURDATE() THEN 0
                        ELSE 1
                    END,

                    CASE p.priority
                        WHEN 'CRITICAL' THEN 0
                        WHEN 'HIGH' THEN 1
                        WHEN 'STANDARD' THEN 2
                        ELSE 3
                    END,

                    pma.date ASC

                LIMIT :recordLimit
                """,
            nativeQuery = true
    )
    List<DueRiskQueueProjection> findDueRiskQueue(
            @Param("departmentId") Long departmentId,
            @Param("upcomingDays") Integer upcomingDays,
            @Param("recordLimit") Integer recordLimit
    );

    @Query(
            value = """
                SELECT
                    pma.id AS assignmentId,
                    pma.project_id AS projectId,

                    m.id AS milestoneId,
                    m.name AS milestoneName,

                    m.id AS displayOrder,

                    ms.id AS statusId,
                    ms.name AS statusName,

                    CASE
                        WHEN pma.status_id = 3 THEN 100
                        WHEN pma.status_id = 2 THEN 50
                        ELSE 0
                    END AS progressPercentage,

                    u.id AS assignedUserId,
                    u.full_name AS assignedUserName,

                    pma.date AS dueDate

                FROM project_milestone_assignment pma

                INNER JOIN milestones m
                        ON m.id = pma.milestone_id

                LEFT JOIN milestone_statuses ms
                       ON ms.id = pma.status_id

                LEFT JOIN users u
                       ON u.id = pma.assigned_user_id

                WHERE pma.project_id IN (:projectIds)
                  AND pma.is_deleted = 0
                  AND pma.is_visible = 1

                ORDER BY
                    pma.project_id ASC,
                    m.id ASC
                """,
            nativeQuery = true
    )
    List<ProjectMilestoneTrackerProjection> findTrackerMilestones(
            @Param("projectIds") List<Long> projectIds
    );
///  //////////////////////////////////////////////////////////////////////////////

/*
@Query(
        value = """
                SELECT
                    pma.id AS assignmentId,

                    u.id AS userId,
                    u.full_name AS userName,

                    p.id AS projectId,
                    p.project_no AS projectNumber,
                    p.name AS projectName,

                    pr.id AS productId,
                    pr.product_name AS productName,

                    m.id AS milestoneId,
                    m.name AS milestoneName,

                    pma.status_id AS statusId,

                    pmm.performance_tat_applicable AS performanceTatApplicable,
                    pmm.performance_tat_hours AS performanceTatHours,

                    pma.started_date AS startedDate,
                    pma.completed_date AS completedDate

                FROM project_milestone_assignment pma

                INNER JOIN project p
                        ON p.id = pma.project_id

                INNER JOIN products pr
                        ON pr.id = p.product_id

                INNER JOIN milestones m
                        ON m.id = pma.milestone_id

                INNER JOIN users u
                        ON u.id = pma.assigned_user_id

                INNER JOIN product_milestone_map pmm
                        ON pmm.id = pma.product_milestone_map_id

                WHERE pma.assigned_user_id = :assignedUserId

                  AND (:projectId IS NULL OR pma.project_id = :projectId)

                  AND pma.is_deleted = 0
                  AND pma.is_visible = 1

                  AND p.is_deleted = 0

                  AND pmm.is_active = 1
                  AND pmm.is_deleted = 0

                ORDER BY
                    p.id ASC,
                    pmm.step_order ASC,
                    pma.id ASC
                """,
        nativeQuery = true
)
List<UserMilestonePerformanceProjection> findUserProjectPerformance(
        @Param("assignedUserId") Long assignedUserId,
        @Param("projectId") Long projectId
);
*/

@Query(
        value = """
                SELECT
                    pma.id AS assignmentId,

                    u.id AS userId,
                    u.full_name AS userName,

                    p.id AS projectId,
                    p.project_no AS projectNumber,
                    p.name AS projectName,

                    pr.id AS productId,
                    pr.product_name AS productName,

                    m.id AS milestoneId,
                    m.name AS milestoneName,

                    pma.status_id AS statusId,

                    pmm.performance_tat_applicable AS performanceTatApplicable,
                    pmm.performance_tat_hours AS performanceTatHours,

                    pma.started_date AS startedDate,
                    pma.completed_date AS completedDate

                FROM project_milestone_assignment pma

                INNER JOIN project p
                        ON p.id = pma.project_id

                INNER JOIN products pr
                        ON pr.id = p.product_id

                INNER JOIN milestones m
                        ON m.id = pma.milestone_id

                INNER JOIN users u
                        ON u.id = pma.assigned_user_id

                LEFT JOIN product_milestone_map pmm
                       ON pmm.id = pma.product_milestone_map_id

                WHERE pma.assigned_user_id = :assignedUserId

                  AND (:projectId IS NULL OR pma.project_id = :projectId)

                  AND pma.is_deleted = 0
                  AND p.is_deleted = 0

                ORDER BY
                    p.id ASC,
                    pma.id ASC
                """,
        nativeQuery = true
)
List<UserMilestonePerformanceProjection> findUserProjectPerformance(
        @Param("assignedUserId") Long assignedUserId,
        @Param("projectId") Long projectId
);

    /**
     * Returns visible milestones belonging to any of the manager's departments.
     *
     * The milestone can be assigned or unassigned.
     */
    @Query("""
        SELECT DISTINCT assignment
        FROM ProjectMilestoneAssignment assignment
        JOIN assignment.milestone milestone
        JOIN milestone.departments department
        WHERE assignment.project.id = :projectId
          AND department.id IN :departmentIds
          AND assignment.isVisible = true
          AND assignment.isDeleted = false
        """)
    List<ProjectMilestoneAssignment> findVisibleMilestonesByProjectAndDepartments(
            @Param("projectId") Long projectId,
            @Param("departmentIds") List<Long> departmentIds
    );









}