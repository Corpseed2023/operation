package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorOnboardingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface VendorOnboardingDocumentRepository
        extends JpaRepository<VendorOnboardingDocument, Long> {

    List<VendorOnboardingDocument>
    findByVendorOnboarding_IdInAndIsDeletedFalseOrderByUploadedDateAsc(
            Collection<Long> onboardingIds
    );
}