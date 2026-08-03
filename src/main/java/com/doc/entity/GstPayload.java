package com.doc.entity;

import java.math.BigDecimal;

public record GstPayload(
        Boolean gstActive,
        String gstRegistrationType,
        String gstSupplyType,
        String gstStateCode,
        BigDecimal gstPercentage
) {
}