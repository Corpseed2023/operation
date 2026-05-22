package com.doc.service.vendor;

import com.doc.dto.vendor.PurchaseOrderRequestDto;
import com.doc.dto.vendor.PurchaseOrderResponseDto;

public interface PurchaseOrderService {

    PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto dto);

    PurchaseOrderResponseDto getPurchaseOrderById(Long id);

    PurchaseOrderResponseDto releasePurchaseOrder(Long poId, Long userId);

    PurchaseOrderResponseDto getByProcurementAssignmentId(Long procurementAssignmentId);

}