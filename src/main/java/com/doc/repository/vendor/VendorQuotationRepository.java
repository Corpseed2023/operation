package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorQuotationRepository extends JpaRepository<VendorQuotation,Long> {

}
