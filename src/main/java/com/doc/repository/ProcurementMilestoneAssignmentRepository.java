package com.doc.repository;

import com.doc.entity.vendor.ProcurementMilestoneAssignment;
import com.doc.entity.project.ProcurementStatus;
import com.doc.repository.projection.VendorAssignmentCountProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcurementMilestoneAssignmentRepository extends JpaRepository<ProcurementMilestoneAssignment, Long> {

    boolean existsByProjectIdAndMilestoneId(Long projectId, Long milestoneId);

    Optional<ProcurementMilestoneAssignment> findByProjectIdAndMilestoneIdAndIsDeletedFalse(
            Long projectId,
            Long milestoneId
    );

    @Query(value = """
    SELECT * FROM procurement_milestone_assignment 
    WHERE project_id = :projectId 
      AND is_deleted = false 
    LIMIT 1
""", nativeQuery = true)
    Optional<ProcurementMilestoneAssignment> findActiveByProjectIdNative(@Param("projectId") Long projectId);

    Optional<ProcurementMilestoneAssignment> findByProjectIdAndIsDeletedFalse(Long projectId);

    List<ProcurementMilestoneAssignment> findByAssignedToIdAndIsDeletedFalse(Long userId);

    List<ProcurementMilestoneAssignment> findByStatusAndIsDeletedFalse(ProcurementStatus status);

    // Option 1: Using JOIN FETCH (cleanest)
    @Query("""
    SELECT pma 
    FROM ProcurementMilestoneAssignment pma 
    LEFT JOIN FETCH pma.project 
    LEFT JOIN FETCH pma.milestone 
    LEFT JOIN FETCH pma.assignedTo 
    LEFT JOIN FETCH pma.selectedVendor 
    WHERE pma.id = :id AND pma.isDeleted = false
""")
    Optional<ProcurementMilestoneAssignment> findByIdAndIsDeletedFalse(@Param("id") Long id);
    @Query("""
    SELECT
        v.id AS vendorId,
        v.name AS vendorName,

        COUNT(pma.id) AS totalAssignmentCount,

        SUM(
            CASE
                WHEN ps.id IN (2, 6)
                THEN 1
                ELSE 0
            END
        ) AS activeCount,

        SUM(
            CASE
                WHEN ps.id = 3
                THEN 1
                ELSE 0
            END
        ) AS completedCount,

        SUM(
            CASE
                WHEN ps.id = 1
                THEN 1
                ELSE 0
            END
        ) AS pendingCount

    FROM ProcurementMilestoneAssignment pma
    JOIN pma.selectedVendor v
    JOIN pma.project p
    JOIN p.status ps
    WHERE pma.isDeleted = false
    GROUP BY v.id, v.name
    ORDER BY v.name ASC
""")
    List<VendorAssignmentCountProjection> getVendorWiseAssignmentCounts();


}