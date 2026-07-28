package com.doc.feign;

import com.doc.dto.vendor.AccountVendorSyncRequestDto;
import com.doc.dto.vendor.AccountVendorSyncResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "account-service",
        path = "/accountService/api/internal/vendors"
)
public interface AccountFeignClient {

    @PostMapping("/sync")
    AccountVendorSyncResponseDto syncVendor(
            @RequestBody AccountVendorSyncRequestDto request
    );
}
