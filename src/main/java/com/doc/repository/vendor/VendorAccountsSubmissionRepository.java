package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorAccountsSubmission;
import com.doc.entity.vendor.VendorAccountsSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorAccountsSubmissionRepository
        extends JpaRepository<VendorAccountsSubmission, Long> {
    Optional<VendorAccountsSubmission> findByIdAndIsDeletedFalse(Long id);

    List<VendorAccountsSubmission>
    findByIsDeletedFalseOrderBySentToAccountsDateDesc();

    boolean existsByVendorFinalization_IdAndStatusInAndIsDeletedFalse(
            Long finalizationId,
            List<VendorAccountsSubmissionStatus> statuses
    );

    @Query("""
        SELECT vas
        FROM VendorAccountsSubmission vas
        JOIN FETCH vas.vendorFinalization vf
        LEFT JOIN FETCH vf.rfq r
        LEFT JOIN FETCH vf.quotation q
        LEFT JOIN FETCH vf.quotationItem qi
        WHERE vf.rfq.product.id = :productId
          AND vas.vendor.id = :vendorId
          AND vas.status = :status
          AND vas.isDeleted = false
          AND vf.isDeleted = false
        ORDER BY
          vas.accountsVerifiedDate DESC,
          vas.sentToAccountsDate DESC,
          vas.id DESC
        """)
    List<VendorAccountsSubmission> findLatestByProductAndVendorAndStatus(
            @Param("productId") Long productId,
            @Param("vendorId") Long vendorId,
            @Param("status") VendorAccountsSubmissionStatus status
    );


}