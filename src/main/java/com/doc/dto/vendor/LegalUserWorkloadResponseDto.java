package com.doc.dto.vendor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LegalUserWorkloadResponseDto {

    private Long userId;
    private String name;
    private long pendingRequestCount;
}