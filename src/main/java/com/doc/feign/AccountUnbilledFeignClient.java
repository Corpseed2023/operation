package com.doc.feign;

import com.doc.dto.CancelUnbilledAndEstimateRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "account-service",
        contextId = "accountUnbilledFeignClient",
        path = "/accountService/api/v1/unbilled-invoices")
public interface AccountUnbilledFeignClient {

    @PutMapping("/{unbilledNumber}/cancel-with-estimate")
    void cancelUnbilledWithEstimate(@PathVariable("unbilledNumber") String unbilledNumber,
                                    @RequestBody CancelUnbilledAndEstimateRequestDto request);
}
