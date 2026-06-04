package com.doc.service.vendor;

import com.doc.dto.vendor.ProcurementPaymentActionRequestDto;
import com.doc.dto.vendor.ProcurementPaymentRequestDto;
import com.doc.dto.vendor.ProcurementPaymentRequestResponseDto;
import com.doc.entity.vendor.PaymentRequestStatus;
import org.springframework.data.domain.Page;

public interface ProcurementPaymentRequestService {

    ProcurementPaymentRequestResponseDto createPaymentRequest(
            Long procurementOrderId,
            ProcurementPaymentRequestDto requestDto
    );

    Page<ProcurementPaymentRequestResponseDto> getPaymentRequestsByStatus(
            PaymentRequestStatus status,
            int page,
            int size
    );

    Page<ProcurementPaymentRequestResponseDto> getPaymentRequestsByProcurementOrderId(
            Long procurementOrderId,
            int page,
            int size
    );


    ProcurementPaymentRequestResponseDto approvePaymentRequest(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    );

    ProcurementPaymentRequestResponseDto rejectPaymentRequest(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    );

    ProcurementPaymentRequestResponseDto releasePayment(
            Long paymentRequestId,
            Long userId,
            ProcurementPaymentActionRequestDto request
    );
}