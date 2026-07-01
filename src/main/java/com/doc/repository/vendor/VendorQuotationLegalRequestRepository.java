package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorQuotationLegalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorQuotationLegalRequestRepository
        extends JpaRepository<VendorQuotationLegalRequest, Long> {

    List<VendorQuotationLegalRequest> findByIsDeletedFalseOrderByCreatedDateDesc();

    List<VendorQuotationLegalRequest> findByAssignedToLegalAndIsDeletedFalseOrderByCreatedDateDesc(
            Long assignedToLegal
    );
    Optional<VendorQuotationLegalRequest>
    findTopByVendorQuotation_IdAndIsDeletedFalseOrderByCreatedDateDesc(Long vendorQuotationId);
}