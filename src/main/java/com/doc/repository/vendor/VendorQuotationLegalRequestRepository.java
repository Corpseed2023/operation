package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorQuotationLegalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorQuotationLegalRequestRepository
        extends JpaRepository<VendorQuotationLegalRequest, Long> {
}