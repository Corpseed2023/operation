package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorFinalization;
import com.doc.entity.vendor.VendorFinalizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

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

    @Query("""
        SELECT COUNT(DISTINCT vf.vendor.id)
        FROM VendorFinalization vf
        WHERE vf.rfq.product.id = :productId
          AND vf.isDeleted = false
          AND vf.vendor.isDeleted = false
          AND vf.rfq.isDeleted = false
          AND vf.status IN :statuses
        """)
    Long countDistinctFinalizedVendorsByProductId(
            @Param("productId") Long productId,
            @Param("statuses") List<VendorFinalizationStatus> statuses
    );

    @Query("""
        SELECT COUNT(DISTINCT v.id)
        FROM ProductVendorMapping m
        JOIN m.product p
        JOIN m.vendor v
        WHERE p.id = :productId
          AND m.isDeleted = false
          AND m.isActive = true
          AND p.isDeleted = false
          AND p.isActive = true
          AND v.isDeleted = false
        """)
    Long countActiveVendorsByProductId(@Param("productId") Long productId);

    @Query("""
        SELECT vf
        FROM VendorFinalization vf
        JOIN FETCH vf.vendor v
        WHERE vf.rfq.product.id = :productId
          AND vf.isDeleted = false
          AND vf.rfq.isDeleted = false
          AND v.isDeleted = false
          AND vf.status IN :statuses
        ORDER BY
          CASE WHEN vf.totalFinalizedAmount IS NULL THEN 1 ELSE 0 END,
          vf.totalFinalizedAmount ASC,
          vf.id ASC
        """)
    List<VendorFinalization> findLowestFinalizedVendorByProductId(
            @Param("productId") Long productId,
            @Param("statuses") List<VendorFinalizationStatus> statuses,
            Pageable pageable
    );

    @Query("""
        SELECT vf
        FROM VendorFinalization vf
        WHERE vf.rfq.product.id = :productId
          AND vf.vendor.id = :vendorId
          AND vf.status = :status
          AND vf.isDeleted = false
        ORDER BY vf.createdDate DESC
        """)
    List<VendorFinalization> findLatestFinalizationByProductAndVendorAndStatus(
            @Param("productId") Long productId,
            @Param("vendorId") Long vendorId,
            @Param("status") VendorFinalizationStatus status
    );


}