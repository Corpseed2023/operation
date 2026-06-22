package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorQuotationRepository extends JpaRepository<VendorQuotation, Long> {

    Optional<VendorQuotation> findByIdAndIsDeletedFalse(Long id);

    List<VendorQuotation> findByIsDeletedFalseOrderByCreatedDateDesc();

    Optional<VendorQuotation> findTopByRfqVendor_IdOrderByVersionNoDesc(Long rfqVendorId);
}