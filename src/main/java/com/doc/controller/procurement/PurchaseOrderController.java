package com.doc.controller.procurement;

import com.doc.dto.vendor.PurchaseOrderRequestDto;
import com.doc.dto.vendor.PurchaseOrderResponseDto;
import com.doc.service.vendor.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/purchase-orders")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @PostMapping
    @Operation(summary = "Create new Purchase Order (starts as DRAFT)")
    public ResponseEntity<PurchaseOrderResponseDto> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequestDto requestDto) {

        PurchaseOrderResponseDto response = purchaseOrderService.createPurchaseOrder(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{poId}/release")
    @Operation(summary = "Release Purchase Order (Change status to RELEASED)")
    public ResponseEntity<PurchaseOrderResponseDto> releasePurchaseOrder(
            @PathVariable Long poId,
            @RequestParam @Parameter(description = "User ID who is releasing the PO") Long userId) {

        PurchaseOrderResponseDto response = purchaseOrderService.releasePurchaseOrder(poId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{poId}")
    @Operation(summary = "Get Purchase Order by ID")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrderById(@PathVariable Long poId) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(poId));
    }

    @GetMapping("/procurement/{procurementAssignmentId}")
    @Operation(summary = "Get Purchase Order by Procurement Assignment ID")
    public ResponseEntity<PurchaseOrderResponseDto> getByProcurementAssignmentId(
            @PathVariable Long procurementAssignmentId) {

        return ResponseEntity.ok(purchaseOrderService.getByProcurementAssignmentId(procurementAssignmentId));
    }
}