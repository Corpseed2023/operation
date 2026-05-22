package com.doc.repository;

import com.doc.entity.project.ProcurementMilestoneAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface ProcurementMilestoneAssignmentRepository extends JpaRepository<ProcurementMilestoneAssignment, Long> {
    boolean existsByProjectIdAndMilestoneId(Long projectId, Long milestoneId);

}