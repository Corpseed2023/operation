package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorQuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorQuotationItemRepository extends JpaRepository<VendorQuotationItem, Long> {

    Optional<VendorQuotationItem> findByIdAndIsDeletedFalse(Long id);
}