package com.doc.dto.account.vendor;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorVoucherRequestDto {

    private AccountVoucherType voucherType;

    private AccountVoucherSourceType sourceType;

    /*
     * Operation Service source record ID.
     */
    private Long sourceId;

    private LocalDate voucherDate;

    private String narration;

    @Builder.Default
    private List<VendorVoucherEntryRequestDto> entries =
            new ArrayList<>();
}