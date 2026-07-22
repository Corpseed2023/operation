package com.doc.repository.vendor;

import com.doc.entity.vendor.RFQ;
import com.doc.repository.projection.ProductVendorDashboardProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.Repository;
@org.springframework.stereotype.Repository
public interface ProductVendorDashboardRepository
        extends Repository<RFQ, Long> {

    @Query(value = """
            SELECT

                /* Registered vendors mapped with product */
                (
                    SELECT COUNT(DISTINCT pvm.vendor_id)
                    FROM product_vendor_mapping pvm
                    WHERE pvm.product_id = :productId
                ) AS registeredVendorCount,

                /* Active RFQs */
                (
                    SELECT COUNT(DISTINCT r.id)
                    FROM rfqs r
                    WHERE r.product_id = :productId
                      AND r.is_deleted = 0
                      AND r.status IN (
                          'SENT',
                          'UNDER_COMPARISON',
                          'ONBOARDING_STARTED'
                      )
                ) AS activeRfqCount,

                /* Quotation received vendor assignments */
                (
                    SELECT COUNT(DISTINCT vq.rfq_vendor_id)
                    FROM vendor_quotations vq
                    INNER JOIN rfqs quotation_rfq
                        ON quotation_rfq.id = vq.rfq_id
                    WHERE quotation_rfq.product_id = :productId
                      AND quotation_rfq.is_deleted = 0
                      AND vq.is_deleted = 0
                      AND vq.is_latest = 1
                      AND vq.status NOT IN (
                          'DRAFT',
                          'CANCELLED'
                      )
                ) AS quotationReceivedCount,

                /* RFQs where price comparison is possible */
                (
                    SELECT COUNT(*)
                    FROM (
                        SELECT comparison_quotation.rfq_id
                        FROM vendor_quotations comparison_quotation

                        INNER JOIN rfqs comparison_rfq
                            ON comparison_rfq.id =
                               comparison_quotation.rfq_id

                        WHERE comparison_rfq.product_id = :productId
                          AND comparison_rfq.is_deleted = 0
                          AND comparison_quotation.is_deleted = 0
                          AND comparison_quotation.is_latest = 1
                          AND comparison_quotation.status IN (
                              'SUBMITTED',
                              'UNDER_COMPARISON',
                              'REVISED',
                              'PARTIALLY_ACCEPTED',
                              'ACCEPTED'
                          )

                        GROUP BY comparison_quotation.rfq_id

                        HAVING COUNT(
                            DISTINCT comparison_quotation.vendor_id
                        ) >= 2
                    ) comparison_result
                ) AS priceComparisonCount,

                /* Selected RFQ vendor assignments */
                (
                    SELECT COUNT(DISTINCT rv.id)
                    FROM rfq_vendors rv

                    INNER JOIN rfqs selected_rfq
                        ON selected_rfq.id = rv.rfq_id

                    WHERE selected_rfq.product_id = :productId
                      AND selected_rfq.is_deleted = 0
                      AND rv.is_deleted = 0
                      AND rv.status = 'SELECTED'
                ) AS vendorSelectedCount
            """,
            nativeQuery = true)
    ProductVendorDashboardProjection getDashboardByProductId(
            @Param("productId") Long productId
    );
}
