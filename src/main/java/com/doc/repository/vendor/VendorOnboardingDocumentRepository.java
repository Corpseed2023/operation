package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorOnboardingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorOnboardingDocumentRepository extends JpaRepository<VendorOnboardingDocument, Long> {
}