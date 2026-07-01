package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorQuotation;
import com.doc.entity.vendor.VendorQuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VendorQuotationRepository extends JpaRepository<VendorQuotation, Long> {

    Optional<VendorQuotation> findByIdAndIsDeletedFalse(Long id);

    /**
     * Recommended query for quotation list by RFQ.
     *
     * Fetches only ManyToOne relations:
     * quotation -> rfq
     * quotation -> vendor
     * quotation -> rfqVendor
     *
     * Do not fetch items + documents together here,
     * because both are List collections and Hibernate can throw MultipleBagFetchException.
     */
    @Query("""
            SELECT DISTINCT q
            FROM VendorQuotation q
            LEFT JOIN FETCH q.rfq r
            LEFT JOIN FETCH q.vendor v
            LEFT JOIN FETCH q.rfqVendor rv
            WHERE r.id = :rfqId
              AND q.isDeleted = false
            ORDER BY q.createdDate DESC
            """)
    List<VendorQuotation> findByRfqIdAndIsDeletedFalseOrderByCreatedDateDesc(
            @Param("rfqId") Long rfqId
    );


    /**
     * Fetch all non-deleted quotations for a specific vendor.
     */
    @Query("""
            SELECT DISTINCT q
            FROM VendorQuotation q
            LEFT JOIN FETCH q.vendor v
            LEFT JOIN FETCH q.rfq r
            LEFT JOIN FETCH q.rfqVendor rv
            WHERE v.id = :vendorId
              AND q.isDeleted = false
            ORDER BY q.createdDate DESC
            """)
    List<VendorQuotation> getQuotationsByVendorId(
            @Param("vendorId") Long vendorId
    );

    /**
     * If you need vendor quotations with items also.
     */
    @Query("""
            SELECT DISTINCT q
            FROM VendorQuotation q
            LEFT JOIN FETCH q.vendor v
            LEFT JOIN FETCH q.rfq r
            LEFT JOIN FETCH q.rfqVendor rv
            LEFT JOIN FETCH q.items i
            WHERE v.id = :vendorId
              AND q.isDeleted = false
            ORDER BY q.createdDate DESC
            """)
    List<VendorQuotation> getQuotationsByVendorIdWithItems(
            @Param("vendorId") Long vendorId
    );

    @Query("""
        SELECT COUNT(vq)
        FROM VendorQuotation vq
        WHERE vq.rfq.product.id = :productId
          AND vq.isDeleted = false
          AND vq.rfq.isDeleted = false
          AND vq.status IN :statuses
        """)
    Long countReceivedQuotationsByProductId(
            @Param("productId") Long productId,
            @Param("statuses") List<VendorQuotationStatus> statuses
    );
}