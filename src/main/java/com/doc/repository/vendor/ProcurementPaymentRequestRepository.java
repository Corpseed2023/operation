package com.doc.repository.vendor;

import com.doc.entity.vendor.PaymentRequestStatus;
import com.doc.entity.vendor.ProcurementOrder;
import com.doc.entity.vendor.ProcurementPaymentRequest;
import com.doc.repository.projection.VendorPaymentSummaryProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProcurementPaymentRequestRepository
        extends JpaRepository<ProcurementPaymentRequest, Long> {

    Page<ProcurementPaymentRequest> findByIsDeletedFalse(Pageable pageable);

    Page<ProcurementPaymentRequest> findByStatusAndIsDeletedFalse(
            PaymentRequestStatus status,
            Pageable pageable
    );

    Optional<ProcurementPaymentRequest>
    findFirstByProcurementOrderAndIsDeletedFalseOrderByCreatedDateAsc(
            ProcurementOrder procurementOrder
    );

    Page<ProcurementPaymentRequest> findByProcurementOrder_IdAndIsDeletedFalse(
            Long procurementOrderId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM ProcurementPaymentRequest p
            WHERE p.id = :paymentRequestId
              AND p.isDeleted = false
            """)
    Optional<ProcurementPaymentRequest> findActiveByIdForUpdate(
            @Param("paymentRequestId") Long paymentRequestId
    );


    /**
     * PO finalAmount is the taxable/basic PO value, therefore reservation is
     * also measured using p.amount (taxable value), not bank payment or net payable.
     * Rejected requests release their reservation.
     */
    @Query("""
        SELECT COALESCE(SUM(COALESCE(p.amount, 0)), 0)
        FROM ProcurementPaymentRequest p
        WHERE p.procurementOrder = :order
          AND p.isDeleted = false
          AND p.status <> com.doc.entity.vendor.PaymentRequestStatus.REJECTED
    """)
    BigDecimal sumReservedTaxableAmountByOrder(
            @Param("order") ProcurementOrder order
    );

    @Query("""
        SELECT
            COALESCE(
                SUM(
                    CASE
                        WHEN p.status =
                            com.doc.entity.vendor.PaymentRequestStatus.PAYMENT_RELEASED
                        THEN COALESCE(p.bankPaymentAmount, p.payableAmount, 0)
                        ELSE 0
                    END
                ),
                0
            ) AS paymentGivenAmount,

            COALESCE(
                SUM(
                    CASE
                        WHEN p.status IN (
                            com.doc.entity.vendor.PaymentRequestStatus.PENDING,
                            com.doc.entity.vendor.PaymentRequestStatus.UNDER_REVIEW,
                            com.doc.entity.vendor.PaymentRequestStatus.APPROVED,
                            com.doc.entity.vendor.PaymentRequestStatus.PAYMENT_PROCESSING,
                            com.doc.entity.vendor.PaymentRequestStatus.ON_HOLD
                        )
                        THEN COALESCE(p.payableAmount, 0)
                        ELSE 0
                    END
                ),
                0
            ) AS pendingPaymentAmount,

            COALESCE(
                SUM(
                    CASE
                        WHEN p.status =
                            com.doc.entity.vendor.PaymentRequestStatus.PAYMENT_RELEASED
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS paymentReleasedCount,

            COALESCE(
                SUM(
                    CASE
                        WHEN p.status IN (
                            com.doc.entity.vendor.PaymentRequestStatus.PENDING,
                            com.doc.entity.vendor.PaymentRequestStatus.UNDER_REVIEW,
                            com.doc.entity.vendor.PaymentRequestStatus.APPROVED,
                            com.doc.entity.vendor.PaymentRequestStatus.PAYMENT_PROCESSING,
                            com.doc.entity.vendor.PaymentRequestStatus.ON_HOLD
                        )
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS pendingPaymentCount

        FROM ProcurementPaymentRequest p
        WHERE p.isDeleted = false
          AND (:vendorId IS NULL OR p.vendor.id = :vendorId)
          AND (
              :productId IS NULL
              OR p.procurementOrder.project.product.id = :productId
          )
    """)
    VendorPaymentSummaryProjection getVendorPaymentSummary(
            @Param("vendorId") Long vendorId,
            @Param("productId") Long productId
    );

    /** Retained temporarily for callers compiled against the old method. */
    @Deprecated
    default BigDecimal sumAmountByProcurementOrderAndIsDeletedFalse(
            ProcurementOrder order
    ) {
        return sumReservedTaxableAmountByOrder(order);
    }





}
