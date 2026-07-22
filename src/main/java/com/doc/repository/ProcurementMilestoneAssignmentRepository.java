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
            pvm.vendor.id AS vendorId,
            pvm.vendor.name AS vendorName,

            COUNT(DISTINCT pma.id) AS totalAssignmentCount,

            COUNT(DISTINCT CASE
                WHEN p.status.id IN (2, 6)
                THEN pma.id
            END) AS activeCount,

            COUNT(DISTINCT CASE
                WHEN p.status.id = 3
                THEN pma.id
            END) AS completedCount,

            COUNT(DISTINCT CASE
                WHEN p.status.id = 1
                THEN pma.id
            END) AS pendingCount

        FROM ProcurementMilestoneAssignment pma
        JOIN pma.project p

        JOIN ProductVendorMapping pvm
            ON pvm.product.id = p.product.id

        WHERE pma.isDeleted = false
          AND p.product.id = :productId

        GROUP BY pvm.vendor.id, pvm.vendor.name
        ORDER BY pvm.vendor.name ASC
    """)
    List<VendorAssignmentCountProjection> getVendorWiseAssignmentCountsByProductId(
            @Param("productId") Long productId
    );


}