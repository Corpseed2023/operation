package com.doc.dto.vendor;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVendorSyncResponseDto {

    private Long externalVendorId;

    private Long operationVendorId;

    private Long ledgerId;

    private String ledgerCode;

    private String ledgerName;

    private String ledgerType;

    private String action;

    private Boolean active;

    private String syncStatus;

    private LocalDateTime syncedAt;

    private String message;

    /*
     * Populated only when paymentApproval was supplied.
     */
    private Boolean voucherCreated;

    private Long voucherId;

    private String voucherNumber;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;
}
