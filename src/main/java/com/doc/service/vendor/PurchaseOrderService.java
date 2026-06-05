package com.doc.service.vendor;

import com.doc.dto.vendor.ProcurementOrderResponseDto;
import com.doc.dto.vendor.PurchaseOrderRequestDto;
import com.doc.dto.vendor.PurchaseOrderResponseDto;
import com.doc.entity.vendor.ProcurementOrderStatus;
import org.springframework.data.domain.Page;

public interface PurchaseOrderService {

    PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto dto);

    PurchaseOrderResponseDto getPurchaseOrderById(Long id);

    PurchaseOrderResponseDto releasePurchaseOrder(Long poId, Long userId);

    PurchaseOrderResponseDto getByProcurementAssignmentId(Long procurementAssignmentId);


    Page<PurchaseOrderResponseDto> getPurchaseOrdersByProjectId(
            Long projectId,
            int page,
            int size
    );

    Page<ProcurementOrderResponseDto> getProcurementOrdersByStatus(
            ProcurementOrderStatus status,
            int page,
            int size
    );

    ProcurementOrderResponseDto approveProcurementOrder(
            Long procurementOrderId,
            Long userId,
            String comment
    );

    ProcurementOrderResponseDto rejectProcurementOrder(
            Long procurementOrderId,
            Long userId,
            String reason
    );

    PurchaseOrderResponseDto updatePurchaseOrder(Long poId, PurchaseOrderRequestDto dto);

    PurchaseOrderResponseDto updatePurchaseOrderStatus(
            Long poId,
            ProcurementOrderStatus status,
            Long userId,
            String remarks
    );
}