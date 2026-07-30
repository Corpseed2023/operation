package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementPaymentActionRequestDto {

    /*
     * Approval or release comment.
     */
    private String comment;

    /*
     * Required when rejecting the payment request.
     */
    private String reason;

    /*
     * Vendor invoice details.
     */
    private String invoiceNumber;

    private LocalDate invoiceDate;

    /*
     * Payment transaction reference supplied during release.
     */
    private String transactionReference;

    /*
     * Payment proof URL or document reference.
     */
    private String paymentProof;
}