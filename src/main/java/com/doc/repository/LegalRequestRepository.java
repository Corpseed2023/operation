package com.doc.repository;

import com.doc.entity.legalrequest.LegalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LegalRequestRepository extends JpaRepository<LegalRequest, Long> {

    @Query(
            value = """
                    SELECT *
                    FROM legal_request lr
                    WHERE lr.is_deleted = false
                    AND lr.legal_status = :status
                    ORDER BY lr.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM legal_request lr
                    WHERE lr.is_deleted = false
                    AND lr.legal_status = :status
                    """,
            nativeQuery = true
    )
    Page<LegalRequest> findAllByStatusNative(
            @Param("status") String status,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT *
                FROM legal_request lr
                WHERE lr.is_deleted = false
                AND lr.legal_status = :status
                AND (
                    lr.created_by = :userId
                    OR lr.assigned_to_legal = :userId
                )
                ORDER BY lr.created_at DESC
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM legal_request lr
                WHERE lr.is_deleted = false
                AND lr.legal_status = :status
                AND (
                    lr.created_by = :userId
                    OR lr.assigned_to_legal = :userId
                )
                """,
            nativeQuery = true
    )
    Page<LegalRequest> findByUserRelatedAndStatusNative(
            @Param("userId") Long userId,
            @Param("status") String status,
            Pageable pageable
    );
    public interface LegalStatusCountProjection {
        String getStatus();
        Long getTotal();
    }

    @Query("""
    SELECT lr.legalStatus AS status, COUNT(lr) AS total
    FROM LegalRequest lr
    WHERE lr.isDeleted = false
    GROUP BY lr.legalStatus
    """)
    List<LegalStatusCountProjection> countGroupedByStatus();

    @Query("""
    SELECT lr.legalStatus AS status, COUNT(lr) AS total
    FROM LegalRequest lr
    WHERE lr.isDeleted = false
      AND lr.assignedToLegal.id = :userId
    GROUP BY lr.legalStatus
    """)
    List<LegalStatusCountProjection> countGroupedByStatusForUser(@Param("userId") Long userId);
}