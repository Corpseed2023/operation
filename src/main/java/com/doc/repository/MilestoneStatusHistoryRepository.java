package com.doc.repository;

import com.doc.entity.milestone.MilestoneStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneStatusHistoryRepository
        extends JpaRepository<MilestoneStatusHistory, Long> {

    /*
     * Existing method:
     * Returns complete status history for one milestone assignment.
     */
    @Query("""
            SELECT h
            FROM MilestoneStatusHistory h
            WHERE h.milestoneAssignment.id = :milestoneAssignmentId
              AND h.isDeleted = false
            ORDER BY h.changeDate ASC, h.id ASC
            """)
    List<MilestoneStatusHistory>
    findByMilestoneAssignmentIdAndIsDeletedFalse(
            @Param("milestoneAssignmentId")
            Long milestoneAssignmentId
    );

    /*
     * Existing project-level method:
     * Returns acknowledgement history from every completed milestone
     * belonging to the specified project.
     */
    @Query("""
            SELECT DISTINCT h
            FROM MilestoneStatusHistory h
            JOIN FETCH h.milestoneAssignment assignment
            LEFT JOIN FETCH assignment.milestone milestone
            LEFT JOIN FETCH assignment.productMilestoneMap productMilestoneMap
            JOIN FETCH h.newStatus newStatus
            LEFT JOIN FETCH h.changedBy changedBy
            WHERE assignment.project.id = :projectId
              AND assignment.isDeleted = false
              AND h.isDeleted = false
              AND UPPER(newStatus.name) = 'COMPLETED'
              AND h.acknowledgementAttachmentUrl IS NOT NULL
              AND TRIM(h.acknowledgementAttachmentUrl) <> ''
            ORDER BY h.changeDate ASC, h.id ASC
            """)
    List<MilestoneStatusHistory>
    findCompletedAcknowledgementsByProjectId(
            @Param("projectId")
            Long projectId
    );
}