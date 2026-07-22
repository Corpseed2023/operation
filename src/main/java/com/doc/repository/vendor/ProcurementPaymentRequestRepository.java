package com.doc.repository.vendor;

import com.doc.entity.vendor.PaymentRequestStatus;
import com.doc.entity.vendor.ProcurementOrder;
import com.doc.entity.vendor.ProcurementPaymentRequest;
import com.doc.repository.projection.VendorPaymentSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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


    @Query("""
        SELECT
            COALESCE(
                SUM(
                    CASE
                        WHEN p.status =
                            com.doc.entity.vendor.PaymentRequestStatus.PAYMENT_RELEASED
                        THEN COALESCE(p.payableAmount, 0)
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

          AND (
              :vendorId IS NULL
              OR p.vendor.id = :vendorId
          )

          AND (
              :productId IS NULL
              OR p.procurementOrder.project.product.id = :productId
          )
        """)
    VendorPaymentSummaryProjection getVendorPaymentSummary(
            @Param("vendorId") Long vendorId,
            @Param("productId") Long productId
    );
}