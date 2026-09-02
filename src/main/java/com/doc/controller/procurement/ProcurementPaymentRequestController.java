package com.doc.controller.procurement;

import com.doc.dto.vendor.ProcurementPaymentActionRequestDto;
import com.doc.dto.vendor.ProcurementPaymentRequestDto;
import com.doc.dto.vendor.ProcurementPaymentRequestResponseDto;
import com.doc.dto.vendor.VendorPaymentTransactionResponseDto;
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

    private final ProcurementPaymentRequestService service;

    @PostMapping("/procurement-order/{procurementOrderId}")
    public ResponseEntity<ProcurementPaymentRequestResponseDto> createPaymentRequest(
            @PathVariable Long procurementOrderId,
            @RequestBody ProcurementPaymentRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createPaymentRequest(procurementOrderId, request));
    }

    @GetMapping
    public ResponseEntity<Page<ProcurementPaymentRequestResponseDto>> getPaymentRequests(
            @RequestParam(required = false) PaymentRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                service.getPaymentRequestsByStatus(status, page, size)
        );
    }

    @GetMapping("/byPurchaseOrderId/{procurementOrderId}")
    public ResponseEntity<Page<ProcurementPaymentRequestResponseDto>> getByOrder(
            @PathVariable Long procurementOrderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                service.getPaymentRequestsByProcurementOrderId(
                        procurementOrderId,
                        page,
                        size
                )
        );
    }

    @PutMapping("/{paymentRequestId}/approve/{userId}")
    public ResponseEntity<ProcurementPaymentRequestResponseDto> approve(
            @PathVariable Long paymentRequestId,
            @PathVariable Long userId,
            @RequestBody(required = false) ProcurementPaymentActionRequestDto request
    ) {
        return ResponseEntity.ok(
                service.approvePaymentRequest(paymentRequestId, userId, request)
        );
    }

    @PutMapping("/{paymentRequestId}/reject/{userId}")
    public ResponseEntity<ProcurementPaymentRequestResponseDto> reject(
            @PathVariable Long paymentRequestId,
            @PathVariable Long userId,
            @RequestBody ProcurementPaymentActionRequestDto request
    ) {
        return ResponseEntity.ok(
                service.rejectPaymentRequest(paymentRequestId, userId, request)
        );
    }

    @PutMapping("/{paymentRequestId}/release-payment/{userId}")
    public ResponseEntity<ProcurementPaymentRequestResponseDto> release(
            @PathVariable Long paymentRequestId,
            @PathVariable Long userId,
            @RequestBody ProcurementPaymentActionRequestDto request
    ) {
        return ResponseEntity.ok(
                service.releasePayment(paymentRequestId, userId, request)
        );
    }


    @GetMapping("/vendor-transactions/by-user/{userId}")
    public ResponseEntity<Page<VendorPaymentTransactionResponseDto>>
    getVendorTransactionsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.getVendorTransactionsByUser(
                        userId,
                        page,
                        size
                )
        );
    }


}
