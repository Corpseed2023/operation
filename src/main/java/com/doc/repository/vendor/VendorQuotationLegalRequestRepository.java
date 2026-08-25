package com.doc.repository.vendor;

import com.doc.entity.vendor.VendorQuotationLegalRequest;
import com.doc.entity.vendor.VendorQuotationLegalRequestStatus;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorQuotationLegalRequestRepository
        extends JpaRepository<VendorQuotationLegalRequest, Long> {

    List<VendorQuotationLegalRequest> findByIsDeletedFalseOrderByCreatedDateDesc();

    List<VendorQuotationLegalRequest> findByAssignedToLegalAndIsDeletedFalseOrderByCreatedDateDesc(
            Long assignedToLegal
    );
    Optional<VendorQuotationLegalRequest>
    findTopByVendorQuotation_IdAndIsDeletedFalseOrderByCreatedDateDesc(Long vendorQuotationId);

    @Query("""
        SELECT r.assignedToLegal AS userId, COUNT(r.id) AS total
        FROM VendorQuotationLegalRequest r
        WHERE r.isDeleted = false
          AND r.status = :status
          AND r.assignedToLegal IN :userIds
        GROUP BY r.assignedToLegal
        """)
    List<LegalRequestCountProjection> countByAssignedToLegalGrouped(
            @Param("status") VendorQuotationLegalRequestStatus status,
            @Param("userIds") Collection<Long> userIds
    );

    interface LegalRequestCountProjection {
        Long getUserId();
        Long getTotal();
    }

}