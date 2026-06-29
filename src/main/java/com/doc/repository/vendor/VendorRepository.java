package com.doc.repository.vendor;

import com.doc.entity.vendor.*;
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

    boolean existsByGstNumberAndIsDeletedFalse(String gstNumber);

    Optional<Vendor> findByIdAndIsDeletedFalse(Long id);

    Page<Vendor> findByIsDeletedFalse(Pageable pageable);

    @Query("""
            SELECT v
            FROM Vendor v
            WHERE v.isDeleted = false
            AND (
                LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%'))               
                OR LOWER(v.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(v.mobile) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(v.gstNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Vendor> searchVendors(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT v 
            FROM Vendor v 
            WHERE v.status = :status 
              AND v.isDeleted = :isDeleted
            """)
    List<Vendor> findAllByStatusAndIsDeletedFalse(
            @Param("status") VendorStatus status,
            @Param("isDeleted") boolean isDeleted);


    boolean existsByPanNumberAndIsDeletedFalse(String panNumber);

//    List<RFQVendor> findByVendorIdAndIsDeletedFalseOrderByCreatedDateDesc(Long vendorId);
//
//    List<VendorQuotation> findByVendorIdAndIsDeletedFalseOrderByCreatedDateDesc(Long vendorId);
//
//    List<VendorFinalization> findByVendorIdAndIsDeletedFalseOrderByCreatedDateDesc(Long vendorId);
//
//    List<VendorOnboarding > findByVendorIdAndIsDeletedFalseOrderByCreatedDateDesc(Long vendorId);

}
