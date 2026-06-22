package com.doc.repository.vendor;

import com.doc.dto.vendor.RFQResponseDto;
import com.doc.entity.vendor.RFQ;
import com.doc.entity.vendor.RFQStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VendorRFQRepository extends JpaRepository<RFQ, Long> {

    Optional<RFQ> findTopByRfqNumberStartingWithOrderByRfqNumberDesc(String prefix);

    @Query(
            value = """
                    SELECT new com.doc.dto.vendor.RFQResponseDto(
                        r.id,
                        r.rfqNumber,
                        r.title,
                        r.description,
                        p.id,
                        p.productName,
                        r.scopeOfWork,
                        r.termsAndConditions,
                        r.deliveryLocation,
                        r.quotationSubmissionDeadline,
                        r.expectedStartDate,
                        r.expectedEndDate,
                        r.contactPersonName,
                        r.contactPersonEmail,
                        r.contactPersonMobile,
                        r.status,
                        r.attachmentUrl,
                        r.createdDate,
                        r.updatedDate,
                        r.createdBy,
                        r.updatedBy,
                        r.isDeleted
                    )
                    FROM RFQ r
                    LEFT JOIN r.product p
                    WHERE r.isDeleted = false
                    AND (:productId IS NULL OR p.id = :productId)
                    AND (:status IS NULL OR r.status = :status)
                    ORDER BY r.createdDate DESC
                    """,
            countQuery = """
                    SELECT COUNT(r)
                    FROM RFQ r
                    LEFT JOIN r.product p
                    WHERE r.isDeleted = false
                    AND (:productId IS NULL OR p.id = :productId)
                    AND (:status IS NULL OR r.status = :status)
                    """
    )
    Page<RFQResponseDto> getAllRFQs(
            @Param("productId") Long productId,
            @Param("status") RFQStatus status,
            Pageable pageable
    );
}