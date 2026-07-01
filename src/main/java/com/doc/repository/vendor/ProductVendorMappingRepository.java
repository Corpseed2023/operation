package com.doc.repository.vendor;

import com.doc.entity.vendor.ProductVendorMapping;
import com.doc.entity.vendor.VendorAccountsSubmissionStatus;
import com.doc.entity.vendor.VendorFinalizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVendorMappingRepository extends JpaRepository<ProductVendorMapping, Long> {

    boolean existsByProductIdAndVendorIdAndIsDeletedFalse(Long productId, Long vendorId);

    Optional<ProductVendorMapping> findByProductIdAndVendorId(
            Long productId,
            Long vendorId
    );

    Optional<ProductVendorMapping> findByIdAndIsDeletedFalse(Long id);

    Page<ProductVendorMapping> findByProductIdAndIsDeletedFalse(Long productId, Pageable pageable);

    @Query("""
            SELECT m
            FROM ProductVendorMapping m
            JOIN FETCH m.product p
            JOIN FETCH m.vendor v
            WHERE p.id = :productId
              AND m.isDeleted = false
              AND m.isActive = true
              AND p.isDeleted = false
              AND p.isActive = true
              AND v.isDeleted = false
            ORDER BY m.createdDate DESC
            """)
    List<ProductVendorMapping> findActiveVendorListByProductId(
            @Param("productId") Long productId
    );

    @Query(
            value = """
                    SELECT m
                    FROM ProductVendorMapping m
                    JOIN m.product p
                    JOIN m.vendor v
                    WHERE p.id = :productId
                      AND m.isDeleted = false
                      AND m.isActive = true
                      AND p.isDeleted = false
                      AND p.isActive = true
                      AND v.isDeleted = false
                    """,
            countQuery = """
                    SELECT COUNT(m)
                    FROM ProductVendorMapping m
                    JOIN m.product p
                    JOIN m.vendor v
                    WHERE p.id = :productId
                      AND m.isDeleted = false
                      AND m.isActive = true
                      AND p.isDeleted = false
                      AND p.isActive = true
                      AND v.isDeleted = false
                    """
    )
    Page<ProductVendorMapping> findActiveVendorsByProductId(
            @Param("productId") Long productId,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT m
        FROM ProductVendorMapping m
        JOIN FETCH m.product p
        JOIN FETCH m.vendor v
        JOIN VendorFinalization vf ON vf.vendor.id = v.id
        WHERE p.id = :productId
          AND m.isDeleted = false
          AND m.isActive = true
          AND p.isDeleted = false
          AND p.isActive = true
          AND v.isDeleted = false
          AND vf.isDeleted = false
          AND vf.status = :status
        ORDER BY m.createdDate DESC
        """)
    List<ProductVendorMapping> findVendorListByProductIdAndFinalizationStatus(
            @Param("productId") Long productId,
            @Param("status") VendorFinalizationStatus status
    );

    @Query("""
        SELECT COUNT(m)
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
        SELECT DISTINCT m
        FROM ProductVendorMapping m
        JOIN FETCH m.product p
        JOIN FETCH m.vendor v
        WHERE p.id = :productId
          AND m.isDeleted = false
          AND m.isActive = true
          AND p.isDeleted = false
          AND p.isActive = true
          AND v.isDeleted = false
          AND EXISTS (
                SELECT 1
                FROM VendorAccountsSubmission vas
                JOIN vas.vendorFinalization vf
                WHERE vas.vendor.id = v.id
                  AND vf.rfq.product.id = p.id
                  AND vas.status = :status
                  AND vas.isDeleted = false
                  AND vf.isDeleted = false
          )
        ORDER BY m.createdDate DESC
        """)
    List<ProductVendorMapping> findVendorListByProductIdAndAccountsSubmissionStatus(
            @Param("productId") Long productId,
            @Param("status") VendorAccountsSubmissionStatus status
    );


}