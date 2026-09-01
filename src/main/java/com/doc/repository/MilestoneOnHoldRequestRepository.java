package com.doc.repository;

import com.doc.entity.milestone.MilestoneOnHoldApprovalStatus;
import com.doc.entity.milestone.MilestoneOnHoldRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MilestoneOnHoldRequestRepository
        extends JpaRepository<MilestoneOnHoldRequest, Long> {

    boolean existsByMilestoneAssignment_IdAndApprovalStatus(
            Long assignmentId,
            MilestoneOnHoldApprovalStatus approvalStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM MilestoneOnHoldRequest r
            JOIN FETCH r.milestoneAssignment a
            JOIN FETCH a.project p
            JOIN FETCH r.requestedBy requestedBy
            JOIN FETCH r.approver approver
            JOIN FETCH r.previousStatus previousStatus
            WHERE r.id = :requestId
            """)
    Optional<MilestoneOnHoldRequest> findByIdForDecision(
            @Param("requestId") Long requestId
    );

    /*
     * Manager access:
     * Returns only requests assigned to this manager.
     */
    @Query(
            value = """
                    SELECT r
                    FROM MilestoneOnHoldRequest r
                    JOIN FETCH r.milestoneAssignment a
                    JOIN FETCH a.project p
                    JOIN FETCH r.requestedBy requestedBy
                    JOIN FETCH r.approver approver
                    JOIN FETCH r.previousStatus previousStatus
                    WHERE r.approver.id = :managerId
                      AND (
                            :approvalStatus IS NULL
                            OR r.approvalStatus = :approvalStatus
                      )
                    ORDER BY r.requestedAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(r)
                    FROM MilestoneOnHoldRequest r
                    WHERE r.approver.id = :managerId
                      AND (
                            :approvalStatus IS NULL
                            OR r.approvalStatus = :approvalStatus
                      )
                    """
    )
    Page<MilestoneOnHoldRequest> findManagerQueue(
            @Param("managerId") Long managerId,
            @Param("approvalStatus")
            MilestoneOnHoldApprovalStatus approvalStatus,
            Pageable pageable
    );

    /*
     * ADMIN and OPERATION_HEAD access:
     * Returns all requests.
     */
    @Query(
            value = """
                    SELECT r
                    FROM MilestoneOnHoldRequest r
                    JOIN FETCH r.milestoneAssignment a
                    JOIN FETCH a.project p
                    JOIN FETCH r.requestedBy requestedBy
                    JOIN FETCH r.approver approver
                    JOIN FETCH r.previousStatus previousStatus
                    WHERE (
                            :approvalStatus IS NULL
                            OR r.approvalStatus = :approvalStatus
                          )
                    ORDER BY r.requestedAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(r)
                    FROM MilestoneOnHoldRequest r
                    WHERE (
                            :approvalStatus IS NULL
                            OR r.approvalStatus = :approvalStatus
                          )
                    """
    )
    Page<MilestoneOnHoldRequest> findAllRequestQueue(
            @Param("approvalStatus")
            MilestoneOnHoldApprovalStatus approvalStatus,
            Pageable pageable
    );
}