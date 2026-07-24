package com.doc.feign;

import com.doc.dto.GovernmentFeePostingRequestDto;
import com.doc.dto.GovernmentFeePostingResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "account-service",
        contextId = "accountExpenseFeignClient",
        path = "/accountService/api/v1/internal/project-expenses"
)
public interface AccountExpenseFeignClient {

    @PostMapping("/government-fee/post")
    GovernmentFeePostingResponseDto postGovernmentFeeExpense(
            @RequestBody GovernmentFeePostingRequestDto request
    );


}