package com.doc.repository;

import com.doc.entity.project.ProjectAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectAssignmentHistoryRepository extends JpaRepository<ProjectAssignmentHistory, Long> {

    /**
     * Find assignment history by:
     * - Company ID
     * - Milestone ID
     * - Department ID (via milestone's departments)
     * - Not deleted
     */
    @Query("SELECT h FROM ProjectAssignmentHistory h " +
            "WHERE h.project.company.id = :companyId " +
            "AND h.milestoneAssignment.milestone.id = :milestoneId " +
            "AND EXISTS (SELECT d FROM h.milestoneAssignment.productMilestoneMap.milestone.departments d WHERE d.id = :departmentId) " +
            "AND h.isDeleted = false")
    List<ProjectAssignmentHistory> findByProjectCompanyIdAndMilestoneIdAndDepartmentId(
            @Param("companyId") Long companyId,
            @Param("milestoneId") Long milestoneId,
            @Param("departmentId") Long departmentId);

    /**
     * Check if a user has EVER been assigned to any milestone in this company + department
     * Used for: "Allow offline user if already dealt with company"
     */
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END " +
            "FROM ProjectAssignmentHistory h " +
            "WHERE h.project.company.id = :companyId " +
            "AND h.assignedUser.id = :userId " +
            "AND EXISTS (SELECT d FROM h.milestoneAssignment.productMilestoneMap.milestone.departments d WHERE d.id = :departmentId) " +
            "AND h.isDeleted = false")
    boolean existsByProjectCompanyIdAndAssignedUserIdAndDepartmentId(
            @Param("companyId") Long companyId,
            @Param("userId") Long userId,
            @Param("departmentId") Long departmentId);
}