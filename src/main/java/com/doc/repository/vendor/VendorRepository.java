package com.doc.repository.vendor;

import com.doc.entity.vendor.Vendor;
import com.doc.entity.vendor.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    boolean existsByVendorCodeAndIsDeletedFalse(String vendorCode);

    boolean existsByGstNumberAndIsDeletedFalse(String gstNumber);

    Optional<Vendor> findByIdAndIsDeletedFalse(Long id);

    Page<Vendor> findByIsDeletedFalse(Pageable pageable);

    @Query("""
            SELECT v
            FROM Vendor v
            WHERE v.isDeleted = false
            AND (
                LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(v.vendorCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(v.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(v.mobile) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(v.gstNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Vendor> searchVendors(@Param("keyword") String keyword, Pageable pageable);

    /*
     * IMPORTANT:
     * Keep this query according to your actual vendor-product mapping table/entity.
     *
     * If your mapping entity name is different, only change the JOIN part.
     */
    @Query("""
            SELECT DISTINCT v
            FROM Vendor v
            JOIN VendorProductMap vpm ON vpm.vendor.id = v.id
            WHERE vpm.product.id = :productId
            AND vpm.isDeleted = false
            AND vpm.isActive = true
            AND v.isDeleted = false
            AND v.status = com.doc.entity.vendor.VendorStatus.ACTIVE
            """)
    List<Vendor> findVendorsByProductId(@Param("productId") Long productId);
}