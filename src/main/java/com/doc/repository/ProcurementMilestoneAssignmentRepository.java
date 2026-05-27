package com.doc.repository;

import com.doc.entity.vendor.ProcurementMilestoneAssignment;
import com.doc.entity.project.ProcurementStatus;
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


}