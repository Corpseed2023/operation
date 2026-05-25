package com.doc.repository;

import com.doc.entity.vendor.ProcurementMilestoneAssignment;
import com.doc.entity.project.ProcurementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcurementMilestoneAssignmentRepository extends JpaRepository<ProcurementMilestoneAssignment, Long> {

    boolean existsByProjectIdAndMilestoneId(Long projectId, Long milestoneId);

    Optional<ProcurementMilestoneAssignment> findByIdAndIsDeletedFalse(Long id);

    Optional<ProcurementMilestoneAssignment> findByProjectIdAndMilestoneIdAndIsDeletedFalse(
            Long projectId,
            Long milestoneId
    );

    Optional<ProcurementMilestoneAssignment> findByProjectIdAndIsDeletedFalse(Long projectId);

    List<ProcurementMilestoneAssignment> findByAssignedToIdAndIsDeletedFalse(Long userId);

    List<ProcurementMilestoneAssignment> findByStatusAndIsDeletedFalse(ProcurementStatus status);
}