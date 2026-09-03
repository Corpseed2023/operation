package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VendorLegalStatusCountDto {
    private String status;
    private long count;
}