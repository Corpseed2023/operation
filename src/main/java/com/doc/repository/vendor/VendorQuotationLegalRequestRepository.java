package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorQuotationLegalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorQuotationLegalRequestRepository
        extends JpaRepository<VendorQuotationLegalRequest, Long> {

    List<VendorQuotationLegalRequest> findByIsDeletedFalseOrderByCreatedDateDesc();

    List<VendorQuotationLegalRequest> findByAssignedToLegalAndIsDeletedFalseOrderByCreatedDateDesc(
            Long assignedToLegal
    );
}