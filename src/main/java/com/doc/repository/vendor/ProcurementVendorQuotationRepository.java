package com.doc.repository.vendor;

import com.doc.entity.vendor.ProcurementVendorQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcurementVendorQuotationRepository
        extends JpaRepository<ProcurementVendorQuotation, Long> {


    List<ProcurementVendorQuotation> findByProcurementAssignmentIdAndIsDeletedFalse(Long procurementAssignmentId);

    Optional<ProcurementVendorQuotation> findByIdAndIsDeletedFalse(Long id);

}