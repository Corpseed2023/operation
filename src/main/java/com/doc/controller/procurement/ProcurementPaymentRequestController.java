package com.doc.controller.procurement;

import com.doc.dto.vendor.ProcurementPaymentActionRequestDto;
import com.doc.dto.vendor.ProcurementPaymentRequestDto;
import com.doc.dto.vendor.ProcurementPaymentRequestResponseDto;
import com.doc.entity.vendor.PaymentRequestStatus;
import com.doc.service.vendor.ProcurementPaymentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/procurement-payment-requests")
@RequiredArgsConstructor
public class ProcurementPaymentRequestController {

    private final ProcurementPaymentRequestService procurementPaymentRequestService;

    @PostMapping("/procurement-order/{procurementOrderId}")
    public ResponseEntity<ProcurementPaymentRequestResponseDto> createPaymentRequest(
            @PathVariable Long procurementOrderId,
            @RequestBody ProcurementPaymentRequestDto requestDto
    ) {
        ProcurementPaymentRequestResponseDto response =
                procurementPaymentRequestService.createPaymentRequest(
                        procurementOrderId,
                        requestDto
                );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ProcurementPaymentRequestResponseDto>> getPaymentRequestsByStatus(
            @RequestParam(required = false) PaymentRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ProcurementPaymentRequestResponseDto> response =
                procurementPaymentRequestService.getPaymentRequestsByStatus(
                        status,
                        page,
                        size
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{paymentRequestId}/approve/{userId}")
    public ResponseEntity<ProcurementPaymentRequestResponseDto> approvePaymentRequest(
            @PathVariable Long paymentRequestId,
            @PathVariable Long userId,
            @RequestBody(required = false) ProcurementPaymentActionRequestDto request
    ) {
        ProcurementPaymentRequestResponseDto response =
                procurementPaymentRequestService.approvePaymentRequest(
                        paymentRequestId,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{paymentRequestId}/reject/{userId}")
    public ResponseEntity<ProcurementPaymentRequestResponseDto> rejectPaymentRequest(
            @PathVariable Long paymentRequestId,
            @PathVariable Long userId,
            @RequestBody ProcurementPaymentActionRequestDto request
    ) {
        ProcurementPaymentRequestResponseDto response =
                procurementPaymentRequestService.rejectPaymentRequest(
                        paymentRequestId,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{paymentRequestId}/release-payment/{userId}")
    public ResponseEntity<ProcurementPaymentRequestResponseDto> releasePayment(
            @PathVariable Long paymentRequestId,
            @PathVariable Long userId,
            @RequestBody(required = false) ProcurementPaymentActionRequestDto request
    ) {
        ProcurementPaymentRequestResponseDto response =
                procurementPaymentRequestService.releasePayment(
                        paymentRequestId,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }
}