package com.doc.calculation.vendor;

import com.doc.entity.vendor.VendorGSTRegistrationType;
import com.doc.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Single source of truth for Operation Service procurement invoice math.
 *
 * Contract used by this workflow:
 * - invoiceAmount is GST-inclusive invoice gross.
 * - GST is extracted from invoice gross.
 * - TDS is calculated on taxable value, excluding separately shown GST.
 * - TDS is rounded to the nearest whole rupee and then stored at scale 2.
 * - One payment request represents a full settlement of one vendor invoice.
 */
@Component
public class ProcurementPaymentCalculator {

    public static final String CALCULATION_VERSION = "PROCUREMENT-V2";

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal ZERO_MONEY =
            BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public ProcurementPaymentCalculation calculateFromInvoiceGross(
            BigDecimal invoiceGrossAmount,
            Boolean gstActive,
            String gstSupplyType,
            BigDecimal gstPercentage,
            Boolean tdsActive,
            BigDecimal tdsPercentage,
            VendorGSTRegistrationType vendorRegistrationType
    ) {
        BigDecimal gross = requirePositiveMoney(
                invoiceGrossAmount,
                "Invoice amount must be greater than zero",
                "ERR_INVALID_INVOICE_AMOUNT"
        );

        boolean applyGst = Boolean.TRUE.equals(gstActive);
        boolean applyTds = Boolean.TRUE.equals(tdsActive);

        BigDecimal gstRate = applyGst
                ? requirePercentage(
                gstPercentage,
                "GST percentage must be greater than zero and not more than 100",
                "ERR_INVALID_GST_PERCENTAGE"
        )
                : zeroRate();

        String normalizedSupplyType = applyGst
                ? normalizeSupplyType(gstSupplyType)
                : null;

        validateNormalInputGstEligibility(
                applyGst,
                vendorRegistrationType
        );

        BigDecimal taxableAmount;
        BigDecimal totalGstAmount;
        BigDecimal cgstAmount = ZERO_MONEY;
        BigDecimal sgstAmount = ZERO_MONEY;
        BigDecimal igstAmount = ZERO_MONEY;

        if (applyGst) {
            BigDecimal divisor = BigDecimal.ONE.add(
                    gstRate.divide(HUNDRED, 8, ROUNDING)
            );

            taxableAmount = gross.divide(
                    divisor,
                    MONEY_SCALE,
                    ROUNDING
            );

            totalGstAmount = gross
                    .subtract(taxableAmount)
                    .setScale(MONEY_SCALE, ROUNDING);

            if ("INTRA_STATE".equals(normalizedSupplyType)) {
                // Calculate the first component from the taxable base. Put any
                // one-paise invoice rounding residual into SGST so all three
                // invariants still reconcile exactly.
                cgstAmount = taxableAmount
                        .multiply(gstRate.divide(new BigDecimal("2"), 8, ROUNDING))
                        .divide(HUNDRED, MONEY_SCALE, ROUNDING);
                sgstAmount = totalGstAmount
                        .subtract(cgstAmount)
                        .setScale(MONEY_SCALE, ROUNDING);
            } else {
                igstAmount = totalGstAmount;
            }
        } else {
            taxableAmount = gross;
            totalGstAmount = ZERO_MONEY;
        }

        BigDecimal tdsRate = applyTds
                ? requirePercentage(
                tdsPercentage,
                "TDS percentage must be greater than zero and not more than 100",
                "ERR_INVALID_TDS_PERCENTAGE"
        )
                : zeroRate();

        BigDecimal tdsBaseAmount = taxableAmount;
        BigDecimal tdsAmount = applyTds
                ? roundTdsToWholeRupee(
                tdsBaseAmount
                        .multiply(tdsRate)
                        .divide(HUNDRED, 6, ROUNDING)
        )
                : ZERO_MONEY;

        BigDecimal vendorNetPayableAmount = gross
                .subtract(tdsAmount)
                .setScale(MONEY_SCALE, ROUNDING);

        if (vendorNetPayableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "Vendor net payable amount must be greater than zero",
                    "ERR_INVALID_VENDOR_NET_PAYABLE"
            );
        }

        ProcurementPaymentCalculation result =
                ProcurementPaymentCalculation.builder()
                        .taxableAmount(taxableAmount)
                        .cgstAmount(cgstAmount)
                        .sgstAmount(sgstAmount)
                        .igstAmount(igstAmount)
                        .totalGstAmount(totalGstAmount)
                        .invoiceGrossAmount(gross)
                        .tdsBaseAmount(tdsBaseAmount)
                        .tdsAmount(tdsAmount)
                        .vendorNetPayableAmount(vendorNetPayableAmount)
                        .settlementAmount(gross)
                        .gstPercentage(gstRate)
                        .tdsPercentage(tdsRate)
                        .gstActive(applyGst)
                        .tdsActive(applyTds)
                        .gstSupplyType(normalizedSupplyType)
                        .calculationVersion(CALCULATION_VERSION)
                        .build();

        validateInvariants(result);
        return result;
    }

    public void assertOptionalMoneyMatches(
            String fieldName,
            BigDecimal supplied,
            BigDecimal calculated,
            String errorCode
    ) {
        if (supplied == null) {
            return;
        }

        BigDecimal normalizedSupplied = money(supplied);
        BigDecimal normalizedCalculated = money(calculated);

        if (normalizedSupplied.compareTo(normalizedCalculated) != 0) {
            throw new ValidationException(
                    fieldName + " mismatch. Expected: "
                            + normalizedCalculated.toPlainString()
                            + ", received: "
                            + normalizedSupplied.toPlainString(),
                    errorCode
            );
        }
    }

    public void assertOptionalRateMatches(
            String fieldName,
            BigDecimal supplied,
            BigDecimal calculated,
            String errorCode
    ) {
        if (supplied == null) {
            return;
        }

        BigDecimal normalizedSupplied = rate(supplied);
        BigDecimal normalizedCalculated = rate(calculated);

        if (normalizedSupplied.compareTo(normalizedCalculated) != 0) {
            throw new ValidationException(
                    fieldName + " mismatch. Expected: "
                            + normalizedCalculated.toPlainString()
                            + ", received: "
                            + normalizedSupplied.toPlainString(),
                    errorCode
            );
        }
    }

    public BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(MONEY_SCALE, ROUNDING);
    }

    public BigDecimal rate(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(RATE_SCALE, ROUNDING);
    }

    private void validateNormalInputGstEligibility(
            boolean gstActive,
            VendorGSTRegistrationType registrationType
    ) {
        if (!gstActive) {
            return;
        }

        if (registrationType == null) {
            throw new ValidationException(
                    "Vendor GST registration type is required when GST is active",
                    "ERR_VENDOR_GST_REGISTRATION_TYPE_REQUIRED"
            );
        }

        /*
         * Follow the existing Operation domain contract: REGISTERED and
         * UNREGISTERED are GST-applicable; SEZ and INTERNATIONAL are
         * zero-rated. Whether an UNREGISTERED case must use RCM is a separate
         * tax-policy decision and must not be inferred by this calculator.
         */
        if (!registrationType.isGstApplicable()) {
            throw new ValidationException(
                    "GST must be inactive for zero-rated vendor registration type: "
                            + registrationType,
                    "ERR_GST_NOT_ALLOWED_FOR_ZERO_RATED_VENDOR"
            );
        }
    }

    private String normalizeSupplyType(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(
                    "GST supply type is required when GST is active",
                    "ERR_GST_SUPPLY_TYPE_REQUIRED"
            );
        }

        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace('/', '_')
                .replace(' ', '_');

        if ("CGST_SGST".equals(normalized)
                || "CGST+SGST".equals(normalized)
                || "CGST_AND_SGST".equals(normalized)) {
            normalized = "INTRA_STATE";
        } else if ("IGST".equals(normalized)) {
            normalized = "INTER_STATE";
        }

        if (!"INTRA_STATE".equals(normalized)
                && !"INTER_STATE".equals(normalized)) {
            throw new ValidationException(
                    "GST supply type must be INTRA_STATE or INTER_STATE",
                    "ERR_INVALID_GST_SUPPLY_TYPE"
            );
        }

        return normalized;
    }

    private BigDecimal requirePositiveMoney(
            BigDecimal value,
            String message,
            String errorCode
    ) {
        BigDecimal normalized = money(value);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(message, errorCode);
        }
        return normalized;
    }

    private BigDecimal requirePercentage(
            BigDecimal value,
            String message,
            String errorCode
    ) {
        BigDecimal normalized = rate(value);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0
                || normalized.compareTo(HUNDRED) > 0) {
            throw new ValidationException(message, errorCode);
        }
        return normalized;
    }

    private BigDecimal roundTdsToWholeRupee(BigDecimal value) {
        return value.setScale(0, ROUNDING)
                .setScale(MONEY_SCALE, ROUNDING);
    }

    private BigDecimal zeroRate() {
        return BigDecimal.ZERO.setScale(RATE_SCALE, ROUNDING);
    }

    private void validateInvariants(ProcurementPaymentCalculation result) {
        BigDecimal reconstructedGross = result.getTaxableAmount()
                .add(result.getTotalGstAmount())
                .setScale(MONEY_SCALE, ROUNDING);

        if (reconstructedGross.compareTo(result.getInvoiceGrossAmount()) != 0) {
            throw new IllegalStateException(
                    "Procurement calculation invariant failed: taxable + GST != invoice gross"
            );
        }

        BigDecimal reconstructedSettlement = result.getVendorNetPayableAmount()
                .add(result.getTdsAmount())
                .setScale(MONEY_SCALE, ROUNDING);

        if (reconstructedSettlement.compareTo(result.getSettlementAmount()) != 0) {
            throw new IllegalStateException(
                    "Procurement calculation invariant failed: bank payable + TDS != settlement"
            );
        }

        BigDecimal reconstructedGst = result.getCgstAmount()
                .add(result.getSgstAmount())
                .add(result.getIgstAmount())
                .setScale(MONEY_SCALE, ROUNDING);

        if (reconstructedGst.compareTo(result.getTotalGstAmount()) != 0) {
            throw new IllegalStateException(
                    "Procurement calculation invariant failed: GST components != total GST"
            );
        }
    }
}
