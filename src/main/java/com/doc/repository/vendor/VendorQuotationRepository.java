package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VendorQuotationRepository extends JpaRepository<VendorQuotation, Long> {

    Optional<VendorQuotation> findByIdAndIsDeletedFalse(Long id);

    List<VendorQuotation> findByIsDeletedFalseOrderByCreatedDateDesc();

    List<VendorQuotation> findByRfq_IdAndIsDeletedFalseOrderByCreatedDateDesc(Long rfqId);

    /**
     * Fetch all non-deleted quotations for a specific vendor.
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
    List<VendorQuotation> getQuotationsByVendorId(@Param("vendorId") Long vendorId);
}