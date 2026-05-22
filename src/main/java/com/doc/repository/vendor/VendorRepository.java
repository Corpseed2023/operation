package com.doc.repository.vendor;

import com.doc.entity.vendor.Vendor;
import com.doc.entity.vendor.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    boolean existsByVendorCodeAndIsDeletedFalse(String vendorCode);

    boolean existsByGstNumberAndIsDeletedFalse(String gstNumber);

    Optional<Vendor> findByIdAndIsDeletedFalse(Long id);

    List<Vendor> findByStatusAndIsDeletedFalse(VendorStatus status);

    Page<Vendor> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE v.isDeleted = false AND " +
            "(LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.vendorCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.gstNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Vendor> searchVendors(String keyword, Pageable pageable);

    // For smart suggestion in Procurement
    @Query("SELECT v FROM Vendor v JOIN v.expertiseProducts p WHERE p.id = :productId AND v.isDeleted = false")
    List<Vendor> findVendorsByProductId(Long productId);
}