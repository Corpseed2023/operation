package com.doc.repository.vendor;

import com.doc.dto.vendor.RFQVendorResponseDto;
import com.doc.entity.vendor.RFQVendor;
import com.doc.repository.projection.ProductRfqDashboardProjection;
import com.doc.repository.projection.QuotationResponseRateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RFQVendorRepository extends JpaRepository<RFQVendor, Long> {

    @Query("""
    SELECT new com.doc.dto.vendor.RFQVendorResponseDto(
        rv.id,
        v.id,
        v.name,
        v.email,
        v.mobile,
        v.gstNumber,
        v.panNumber,
        CAST(v.status AS string)
    )
    FROM RFQVendor rv
    JOIN rv.vendor v
    WHERE rv.rfq.id = :rfqId
    AND rv.isDeleted = false
    ORDER BY rv.id DESC
""")
    List<RFQVendorResponseDto> findVendorsByRfqId(@Param("rfqId") Long rfqId);

    @Query("""
    SELECT new com.doc.dto.vendor.RFQVendorResponseDto(
        rv.id,
        v.id,
        v.name,
        v.email,
        v.mobile,
        v.gstNumber,
        v.panNumber,
        CAST(v.status AS string)
    )
    FROM RFQVendor rv
    JOIN rv.vendor v
    WHERE rv.rfq.id = :rfqId
    AND v.id = :vendorId
    AND rv.isDeleted = false
""")
    Optional<RFQVendorResponseDto> findVendorByRfqIdAndVendorId(
            @Param("rfqId") Long rfqId,
            @Param("vendorId") Long vendorId
    );
    List<RFQVendor> findByVendor_IdAndIsDeletedFalseOrderByCreatedDateDesc(Long vendorId);

    @Query(value = """
            SELECT
                r.id AS rfqId,
                r.rfq_number AS rfqNumber,
                r.title AS title,
                r.quotation_submission_deadline
                    AS quotationSubmissionDeadline,

                (
                    SELECT COUNT(DISTINCT rv.vendor_id)
                    FROM rfq_vendors rv
                    WHERE rv.rfq_id = r.id
                      AND rv.is_deleted = 0
                ) AS vendorsInvited,

                (
                    SELECT COUNT(DISTINCT vq.rfq_vendor_id)
                    FROM vendor_quotations vq
                    WHERE vq.rfq_id = r.id
                      AND vq.is_deleted = 0
                      AND vq.is_latest = 1
                      AND vq.status NOT IN (
                          'DRAFT',
                          'CANCELLED'
                      )
                ) AS quotationsReceived,

                r.status AS status

            FROM rfqs r

            WHERE r.is_deleted = 0
              AND r.product_id = :productId

            ORDER BY r.created_date DESC
            """,
            nativeQuery = true)
    List<ProductRfqDashboardProjection>
    findRfqDashboardByProductId(
            @Param("productId") Long productId
    );

    @Query("""
            SELECT
                COUNT(DISTINCT rv.id) AS totalInvited,

                COUNT(DISTINCT CASE
                    WHEN vq.id IS NOT NULL THEN rv.id
                END) AS responded

            FROM RFQVendor rv

            JOIN rv.rfq r

            LEFT JOIN VendorQuotation vq
                ON vq.rfqVendor.id = rv.id
                AND vq.isDeleted = false
                AND vq.isLatest = true

            WHERE r.product.id = :productId
              AND r.isDeleted = false
              AND rv.isDeleted = false
            """)
    QuotationResponseRateProjection getQuotationResponseRate(
            @Param("productId") Long productId
    );
}