package com.doc.repository;

import com.doc.entity.milestone.MilestoneStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneStatusHistoryRepository extends JpaRepository<MilestoneStatusHistory, Long> {

    @Query("SELECT h FROM MilestoneStatusHistory h WHERE h.milestoneAssignment.id = :milestoneAssignmentId AND h.isDeleted = false")
    List<MilestoneStatusHistory> findByMilestoneAssignmentIdAndIsDeletedFalse(@Param("milestoneAssignmentId") Long milestoneAssignmentId);

}