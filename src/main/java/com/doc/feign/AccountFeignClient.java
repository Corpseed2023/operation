package com.doc.feign;

import com.doc.dto.vendor.AccountVendorSyncRequestDto;
import com.doc.dto.vendor.AccountVendorSyncResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
}
