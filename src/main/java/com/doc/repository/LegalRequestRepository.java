package com.doc.repository;

import com.doc.entity.legalrequest.LegalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LegalRequestRepository extends JpaRepository<LegalRequest, Long> {

    // ADMIN: fetch all legal requests by status
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


    // LEGAL USER: fetch requests assigned to legal user
    @Query(
            value = """
                    SELECT *
                    FROM legal_request lr
                    WHERE lr.is_deleted = false
                    AND lr.legal_status = :status
                    AND lr.assigned_to_legal = :userId
                    ORDER BY lr.created_at DESC
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


    // NORMAL USER: fetch own initiated requests
    @Query(
            value = """
                    SELECT *
                    FROM legal_request lr
                    WHERE lr.is_deleted = false
                    AND lr.legal_status = :status
                    AND lr.created_by = :userId
                    ORDER BY lr.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM legal_request lr
                    WHERE lr.is_deleted = false
                    AND lr.legal_status = :status
                    AND lr.created_by = :userId
                    """,
            nativeQuery = true
    )
    Page<LegalRequest> findByCreatedByAndStatusNative(
            @Param("userId") Long userId,
            @Param("status") String status,
            Pageable pageable
    );


    // Optional: fetch both created and assigned records
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
}