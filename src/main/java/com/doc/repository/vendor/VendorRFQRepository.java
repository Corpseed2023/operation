package com.doc.repository.vendor;

import com.doc.dto.vendor.RFQResponseDto;
import com.doc.entity.vendor.RFQ;
import com.doc.entity.vendor.RFQStatus;
import com.doc.entity.vendor.VendorQuotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VendorRFQRepository extends JpaRepository<RFQ, Long> {

    Optional<RFQ> findTopByRfqNumberStartingWithOrderByRfqNumberDesc(String prefix);

    @Query("""
            SELECT r
            FROM RFQ r
            LEFT JOIN FETCH r.product p
            WHERE r.isDeleted = false
            AND (:productId IS NULL OR p.id = :productId)
            AND (:status IS NULL OR r.status = :status)
            AND (:userId IS NULL OR r.createdBy = :userId)
            ORDER BY r.createdDate DESC
            """)
    Page<RFQ> getAllRFQs(
            @Param("productId") Long productId,
            @Param("status") RFQStatus status,
            @Param("userId") Long userId,
            Pageable pageable
    );

}