package com.doc.repository.vendor;

import com.doc.entity.vendor.PaymentRequestStatus;
import com.doc.entity.vendor.ProcurementOrder;
import com.doc.entity.vendor.ProcurementPaymentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcurementPaymentRequestRepository extends JpaRepository<ProcurementPaymentRequest, Long> {

    Page<ProcurementPaymentRequest> findByIsDeletedFalse(Pageable pageable);

    Page<ProcurementPaymentRequest> findByStatusAndIsDeletedFalse(
            PaymentRequestStatus status,
            Pageable pageable
    );

    Optional<ProcurementPaymentRequest> findByProcurementOrderAndIsDeletedFalse(
            ProcurementOrder procurementOrder
    );


    Page<ProcurementPaymentRequest> findByProcurementOrder_IdAndIsDeletedFalse(
            Long procurementOrderId,
            Pageable pageable
    );


}