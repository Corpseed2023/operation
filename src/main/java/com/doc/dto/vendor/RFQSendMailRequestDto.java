package com.doc.dto.vendor;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RFQSendMailRequestDto {

    /**
     * If empty/null, RFQ will be sent to all vendors mapped with this RFQ.
     * If provided, RFQ will be sent only to selected rfqVendorIds.
     */
    private List<Long> rfqVendorIds;

    /**
     * Optional custom mail subject.
     * If empty, backend will generate default subject.
     */
    private String subject;

    /**
     * Optional custom message to add above RFQ details.
     */
    private String message;

    /**
     * Optional CC list.
     */
    private List<String> cc;

    /**
     * Optional BCC list.
     */
    private List<String> bcc;
}