package com.doc.repository.department;

import com.doc.entity.department.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Team entities.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * Finds non-deleted teams by department ID.
     */
    List<Team> findByDepartmentIdAndIsDeletedFalse(Long departmentId);

    /**
     * Finds a non-deleted team by ID.
     */
    Optional<Team> findByIdAndIsDeletedFalse(Long id);

    /**
     * Checks if a team name exists in a department.
     */
    boolean existsByNameAndDepartmentIdAndIsDeletedFalse(String name, Long departmentId);

    /**
     * Finds temporary teams whose end date has passed and are not deleted.
     */
    @Query("SELECT t FROM Team t WHERE t.isTemporary = true AND t.isDeleted = false AND t.endDate <= :currentDate")
    List<Team> findTemporaryTeamsToDelete(@Param("currentDate") Date currentDate);

    /**
     * Finds active teams handling a specific product in a department.
     */
    @Query("SELECT t FROM Team t JOIN t.products p WHERE p.id = :productId AND t.department.id = :departmentId AND t.isDeleted = false AND t.isActive = true")
    List<Team> findByProductsIdAndDepartmentIdAndIsDeletedFalse(@Param("productId") Long productId, @Param("departmentId") Long departmentId);

    /**
     * Checks if a user is a team lead for a project’s milestones.
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Team t JOIN t.members m JOIN ProjectMilestoneAssignment pma " +
            "WHERE t.teamLead.id = :userId AND pma.project.id = :projectId AND pma.assignedUser.id = m.id AND t.isDeleted = false")
    boolean isTeamLeadForProject(@Param("userId") Long userId, @Param("projectId") Long projectId);
}