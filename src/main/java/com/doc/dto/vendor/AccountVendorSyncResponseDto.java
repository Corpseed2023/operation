package com.doc.dto.vendor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountVendorSyncResponseDto {

    private Long id;

    private Long operationVendorId;

    private String vendorName;

    private Boolean active;
}
