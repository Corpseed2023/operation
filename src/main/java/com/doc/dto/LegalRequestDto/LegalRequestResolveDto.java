package com.doc.dto.LegalRequestDto;

import com.doc.em.LegalRequestStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LegalRequestResolveDto {

    private LegalRequestStatus status; // APPROVED_REFUND, NON_REFUNDED, SERVICE_CHANGE_APPROVED, etc.

    private String statusReason;

    private Long resolvedById;
}