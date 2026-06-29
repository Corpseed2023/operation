package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorFinalization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorFinalizationRepository extends JpaRepository<VendorFinalization, Long> {

    Optional<VendorFinalization> findByIdAndIsDeletedFalse(Long id);

    List<VendorFinalization> findByRfq_IdAndIsDeletedFalseOrderByCreatedDateDesc(Long rfqId);

    boolean existsByRfqVendor_IdAndQuotationItem_IdAndIsDeletedFalse(
            Long rfqVendorId,
            Long quotationItemId
    );

    List<VendorFinalization>
    findBySentToAccountsTrueAndIsDeletedFalseOrderBySentToAccountsDateDesc();

    List<VendorFinalization> findByVendor_IdAndIsDeletedFalseOrderByCreatedDateDesc(Long vendorId);
}