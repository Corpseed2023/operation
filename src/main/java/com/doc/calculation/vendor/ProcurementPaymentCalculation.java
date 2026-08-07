package com.doc.calculation.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Immutable backend-calculated snapshot for one procurement vendor invoice.
 *
 * taxableAmount + totalGstAmount = invoiceGrossAmount
 * invoiceGrossAmount - tdsAmount = vendorNetPayableAmount
 * vendorNetPayableAmount + tdsAmount = settlementAmount
 */
@Getter
@Builder
@AllArgsConstructor
public class ProcurementPaymentCalculation {

    private final BigDecimal taxableAmount;
    private final BigDecimal cgstAmount;
    private final BigDecimal sgstAmount;
    private final BigDecimal igstAmount;
    private final BigDecimal totalGstAmount;
    private final BigDecimal invoiceGrossAmount;
    private final BigDecimal tdsBaseAmount;
    private final BigDecimal tdsAmount;
    private final BigDecimal vendorNetPayableAmount;
    private final BigDecimal settlementAmount;
    private final BigDecimal gstPercentage;
    private final BigDecimal tdsPercentage;
    private final Boolean gstActive;
    private final Boolean tdsActive;
    private final String gstSupplyType;
    private final String calculationVersion;
}
