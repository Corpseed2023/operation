package com.doc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentFeePostingResponseDto {
    private String postingStatus;
    private String message;
    private Long operationExpenseId;
    private Long receiptVoucherId;
    private String receiptVoucherNumber;
    private Long journalVoucherId;
    private String journalVoucherNumber;
    private Long receivingBankLedgerId;
    private Long clientAdvanceLedgerId;
    private Long governmentFeeExpenseLedgerId;
    private Long governmentFeePayableLedgerId;

    /** Legacy aliases mapped to the Step 3 journal voucher. */
    private Long voucherId;
    private String voucherNumber;
    private LocalDateTime postedAt;
}
