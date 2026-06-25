package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorAgreementDecisionRequestDto {
    private String decision; // AGREED / DISAGREED
    private Long decisionBy;
    private String remarks;
}
