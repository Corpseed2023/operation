package com.doc.dto.account.vendor;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorVoucherEntryRequestDto {

    private VendorVoucherLedgerSource ledgerSource;

    /*
     * Required only when ledgerSource = EXISTING_LEDGER.
     */
    private Long ledgerId;

    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    private String narration;
}