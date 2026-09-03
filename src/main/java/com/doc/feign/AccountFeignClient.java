package com.doc.feign;

import com.doc.dto.legalDashbaord.PaymentLegalClientDto;
import com.doc.dto.vendor.AccountVendorSyncRequestDto;
import com.doc.dto.vendor.AccountVendorSyncResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "account-service",
        path = "/accountService/api/v1/internal/vendors"
)
public interface AccountFeignClient {

    /*
     * Same endpoint is used for:
     *
     * 1. Vendor onboarding:
     *    request.paymentApproval == null
     *
     * 2. Vendor payment approval:
     *    request.paymentApproval != null
     *
     * Account Service calculates GST/TDS and creates the voucher entries.
     */
    @PostMapping("/sync")
    AccountVendorSyncResponseDto syncVendor(
            @RequestBody AccountVendorSyncRequestDto request
    );

    @GetMapping("/accountService/api/v1/payment-legal-verification/pending")
    List<PaymentLegalClientDto> getPendingPaymentLegalRequests(
            @RequestParam("userId") Long userId
    );
}
