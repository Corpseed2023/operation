package com.doc.feign;

import com.doc.dto.GovernmentFeeFundTransferPostingRequestDto;
import com.doc.dto.GovernmentFeeFundTransferPostingResponseDto;
import com.doc.dto.GovernmentFeePostingRequestDto;
import com.doc.dto.GovernmentFeePostingResponseDto;
import com.doc.dto.GovernmentFeePaymentPostingResponseDto;
import com.doc.dto.project.GovernmentFeePaymentPostingRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "account-service",
        contextId = "accountExpenseFeignClient",
        path = "/accountService/api/v1/internal/project-expenses"
)
public interface AccountExpenseFeignClient {

    /** Step 3 approval-time posting. */
    @PostMapping("/government-fee/post")
    GovernmentFeePostingResponseDto postGovernmentFeeExpense(
            @RequestBody GovernmentFeePostingRequestDto request
    );

    /** Step 4 HDFC/Kotak -> Axis CONTRA posting. */
    @PostMapping("/government-fee/fund-transfer")
    GovernmentFeeFundTransferPostingResponseDto postGovernmentFeeFundTransfer(
            @RequestBody GovernmentFeeFundTransferPostingRequestDto request
    );

    /** Step 5: Dr Government Fee Payable / Cr payment bank. */
    @PostMapping("/government-fee/payment")
    GovernmentFeePaymentPostingResponseDto postGovernmentFeePayment(
            @RequestBody GovernmentFeePaymentPostingRequestDto request
    );
}
