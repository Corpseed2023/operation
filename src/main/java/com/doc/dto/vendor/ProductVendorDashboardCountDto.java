package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVendorDashboardCountDto {

    private Long productId;

    private Long totalVendorCount;

    private Long totalFinalVendorCount;

    private Long activeRfqCount;

    private Long quotationReceivedCount;

    private Long lowestFinalizedVendorId;

    private String lowestFinalizedVendorName;

    private BigDecimal lowestFinalizedPrice;
}