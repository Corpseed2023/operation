package com.doc.dto.vendor;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class ProductQuotationResponseRate {

    private Long productId;

    private Long totalInvited;

    private Long responded;

    private Long pending;

    private BigDecimal responseRate;
}
