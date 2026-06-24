package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorOnboardingRepository extends JpaRepository<VendorOnboarding, Long> {

    Optional<VendorOnboarding> findByVendorFinalization_IdAndIsDeletedFalse(Long vendorFinalizationId);

    Optional<VendorOnboarding> findTopByOnboardingNumberStartingWithOrderByOnboardingNumberDesc(String prefix);
}