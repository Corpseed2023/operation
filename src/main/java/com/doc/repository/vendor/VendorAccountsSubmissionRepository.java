package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorAccountsSubmission;
import com.doc.entity.vendor.VendorAccountsSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorAccountsSubmissionRepository
        extends JpaRepository<VendorAccountsSubmission, Long> {

    boolean existsByVendorFinalization_IdAndIsDeletedFalse(Long finalizationId);

    Optional<VendorAccountsSubmission> findByIdAndIsDeletedFalse(Long id);

    Optional<VendorAccountsSubmission>
    findByVendorFinalization_IdAndIsDeletedFalse(Long finalizationId);

    List<VendorAccountsSubmission>
    findByIsDeletedFalseOrderBySentToAccountsDateDesc();

    boolean existsByVendorFinalization_IdAndStatusInAndIsDeletedFalse(
            Long finalizationId,
            List<VendorAccountsSubmissionStatus> statuses
    );
}