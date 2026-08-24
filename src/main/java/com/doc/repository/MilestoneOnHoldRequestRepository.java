package com.doc.repository;

import com.doc.entity.milestone.MilestoneOnHoldApprovalStatus;
import com.doc.entity.milestone.MilestoneOnHoldRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface MilestoneOnHoldRequestRepository
        extends JpaRepository<MilestoneOnHoldRequest, Long> {

    boolean existsByMilestoneAssignment_IdAndApprovalStatus(
            Long assignmentId,
            MilestoneOnHoldApprovalStatus approvalStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from MilestoneOnHoldRequest r
            join fetch r.milestoneAssignment a
            join fetch a.project p
            join fetch r.requestedBy requestedBy
            join fetch r.approver approver
            join fetch r.previousStatus previousStatus
            where r.id = :requestId
            """)
    Optional<MilestoneOnHoldRequest> findByIdForDecision(
            @Param("requestId") Long requestId
    );

    @Query(
            value = """
                    select r
                    from MilestoneOnHoldRequest r
                    join fetch r.milestoneAssignment a
                    join fetch a.project p
                    join fetch r.requestedBy requestedBy
                    join fetch r.approver approver
                    join fetch r.previousStatus previousStatus
                    where r.approver.id = :managerId
                      and (:approvalStatus is null or r.approvalStatus = :approvalStatus)
                    order by r.requestedAt desc
                    """,
            countQuery = """
                    select count(r)
                    from MilestoneOnHoldRequest r
                    where r.approver.id = :managerId
                      and (:approvalStatus is null or r.approvalStatus = :approvalStatus)
                    """
    )
    Page<MilestoneOnHoldRequest> findManagerQueue(
            @Param("managerId") Long managerId,
            @Param("approvalStatus") MilestoneOnHoldApprovalStatus approvalStatus,
            Pageable pageable
    );
}
