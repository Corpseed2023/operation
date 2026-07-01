package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorFinalization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VendorFinalizationRepository extends JpaRepository<VendorFinalization, Long> {

    Optional<VendorFinalization> findByIdAndIsDeletedFalse(Long id);

    List<VendorFinalization> findByRfq_IdAndIsDeletedFalseOrderByCreatedDateDesc(Long rfqId);

    boolean existsByRfqVendor_IdAndQuotationItem_IdAndIsDeletedFalse(
            Long rfqVendorId,
            Long quotationItemId
    );

    List<VendorFinalization> findByVendor_IdAndIsDeletedFalseOrderByCreatedDateDesc(Long vendorId);

    @Query("""
            SELECT vf
            FROM VendorFinalization vf
            JOIN vf.quotationItem qi
            WHERE vf.rfq.id = :rfqId
              AND vf.isDeleted = false
              AND LOWER(qi.itemName) = LOWER(:itemName)
            ORDER BY
              CASE WHEN vf.finalizedUnitRate IS NULL THEN 1 ELSE 0 END,
              vf.finalizedUnitRate ASC,
              vf.totalFinalizedAmount ASC,
              vf.id ASC
            """)
    List<VendorFinalization> findComparableFinalizationsForLRanking(
            @Param("rfqId") Long rfqId,
            @Param("itemName") String itemName
    );

    @Query("""
            SELECT vf
            FROM VendorFinalization vf
            WHERE vf.rfq.product.id = :productId
              AND vf.vendor.id = :vendorId
              AND vf.isDeleted = false
            ORDER BY vf.createdDate DESC
            """)
    List<VendorFinalization> findLatestFinalizationByProductAndVendor(
            @Param("productId") Long productId,
            @Param("vendorId") Long vendorId
    );
}