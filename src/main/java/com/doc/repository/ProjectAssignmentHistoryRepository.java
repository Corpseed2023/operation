package com.doc.repository;

import com.doc.entity.project.ProjectAssignmentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProjectAssignmentHistoryRepository extends JpaRepository<ProjectAssignmentHistory, Long> {

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


    @Query("""
    SELECT h FROM ProjectAssignmentHistory h 
    JOIN h.project p
    JOIN h.milestoneAssignment ma
    JOIN ma.milestone m
    JOIN m.departments d
    WHERE p.company.id = :companyId
      AND m.id = :milestoneId
      AND d.id = :departmentId
      AND h.isDeleted = false
    ORDER BY h.createdDate DESC
    """)
    List<ProjectAssignmentHistory> findLatestByCompanyMilestoneDept(
            @Param("companyId") Long companyId,
            @Param("milestoneId") Long milestoneId,
            @Param("departmentId") Long departmentId
    );


    @Query("SELECT h FROM ProjectAssignmentHistory h WHERE h.milestoneAssignment.id = :milestoneAssignmentId AND h.isDeleted = false")
    List<ProjectAssignmentHistory> findByMilestoneAssignmentIdAndIsDeletedFalse(@Param("milestoneAssignmentId") Long milestoneAssignmentId);

}