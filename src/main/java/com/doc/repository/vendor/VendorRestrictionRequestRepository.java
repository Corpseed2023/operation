package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorRestrictionRequest;
import com.doc.entity.vendor.VendorRestrictionRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface VendorRestrictionRequestRepository
        extends JpaRepository<VendorRestrictionRequest, Long> {

    boolean existsByVendor_IdAndStatusIn(
            Long vendorId,
            Collection<VendorRestrictionRequestStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT request
            FROM VendorRestrictionRequest request
            JOIN FETCH request.vendor
            WHERE request.id = :requestId
            """)
    Optional<VendorRestrictionRequest> findByIdForUpdate(
            @Param("requestId") Long requestId
    );

    @Query(
            value = """
                SELECT request
                FROM VendorRestrictionRequest request
                JOIN FETCH request.vendor vendor
                WHERE request.status = :status
                ORDER BY request.requestedAt DESC
                """,
            countQuery = """
                SELECT COUNT(request)
                FROM VendorRestrictionRequest request
                WHERE request.status = :status
                """
    )
    Page<VendorRestrictionRequest> findRequestsByStatus(
            @Param("status")
            VendorRestrictionRequestStatus status,
            Pageable pageable
    );
}