package com.doc.repository.vendor;

import com.doc.entity.vendor.RFQVendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RFQVendorRepository extends JpaRepository<RFQVendor, Long> {
}
