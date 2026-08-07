package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementPaymentActionRequestDto {

    /*
     * Used for APPROVE / RELEASE.
     */
    private String comment;

    /*
     * Used only for REJECT.
     */
    private String reason;

    /*
     * Payment execution information.
     */
    private LocalDate paymentDate;
    private String paymentMode;
    private Long bankLedgerId;

    private String transactionReference;
    private String paymentProof;
    private List<String> proofAttachmentUrls;
}