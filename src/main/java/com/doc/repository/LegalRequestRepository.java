package com.doc.repository;

import com.doc.entity.legalrequest.LegalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LegalRequestRepository extends JpaRepository<LegalRequest, Long> {
    @Query(
            value = """
                    SELECT *
                    FROM legal_request lr
                    WHERE lr.is_deleted = false
                    AND lr.legal_status = :status
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
                    AND lr.assigned_to_legal = :userId
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM legal_request lr
                    WHERE lr.is_deleted = false
                    AND lr.legal_status = :status
                    AND lr.assigned_to_legal = :userId
                    """,
            nativeQuery = true
    )
    Page<LegalRequest> findByAssignedLegalAndStatusNative(
            @Param("userId") Long userId,
            @Param("status") String status,
            Pageable pageable
    );






}