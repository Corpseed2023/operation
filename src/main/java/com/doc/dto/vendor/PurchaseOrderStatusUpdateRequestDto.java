package com.doc.dto.vendor;

import com.doc.entity.vendor.ProcurementOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseOrderStatusUpdateRequestDto {

    @NotNull(message = "Status is required")
    private ProcurementOrderStatus status;

    @NotNull(message = "User ID is required")
    private Long userId;

    private String remarks;
}