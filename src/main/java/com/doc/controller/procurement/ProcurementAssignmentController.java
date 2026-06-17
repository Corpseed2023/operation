package com.doc.controller.procurement;

import com.doc.dto.procurement.ProcurementAssignmentResponseDto;
import com.doc.dto.procurement.SelectProcurementVendorRequestDto;
import com.doc.dto.vendor.request.AddVendorQuotationRequestDto;
import com.doc.dto.vendor.request.SelectVendorQuotationRequestDto;
import com.doc.service.ProcurementAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/procurement-assignments")
public class ProcurementAssignmentController {

    @Autowired
    private ProcurementAssignmentService procurementAssignmentService;

    @GetMapping("/{procurementAssignmentId}")
    @Operation(summary = "Get procurement assignment with eligible vendors")
    public ProcurementAssignmentResponseDto getProcurementAssignment(
            @PathVariable Long procurementAssignmentId
    ) {
        return procurementAssignmentService.getProcurementAssignment(procurementAssignmentId);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get procurement assignment by project ID")
    public ProcurementAssignmentResponseDto getProcurementAssignmentByProject(
            @PathVariable Long projectId
    ) {
        return procurementAssignmentService.getProcurementAssignmentByProject(projectId);
    }

    @GetMapping("/assigned-user/{userId}")
    @Operation(summary = "Get procurement assignments assigned to user")
    public List<ProcurementAssignmentResponseDto> getProcurementAssignmentsByUser(
            @PathVariable Long userId
    ) {
        return procurementAssignmentService.getProcurementAssignmentsByUser(userId);
    }

    @GetMapping("/vendor-required")
    @Operation(summary = "Get procurement assignments where vendor needs to be created")
    public List<ProcurementAssignmentResponseDto> getVendorRequiredAssignments() {
        return procurementAssignmentService.getVendorRequiredAssignments();
    }

    @PutMapping("/{procurementAssignmentId}/vendor")
    @Operation(summary = "Select final vendor for procurement assignment")
    public ProcurementAssignmentResponseDto selectVendor(
            @PathVariable Long procurementAssignmentId,
            @Valid @RequestBody SelectProcurementVendorRequestDto requestDto
    ) {
        return procurementAssignmentService.selectVendor(procurementAssignmentId, requestDto);
    }


    @PostMapping("/{procurementAssignmentId}/vendor-quotations")
    @Operation(summary = "Add vendor quotation for procurement assignment")
    public ProcurementAssignmentResponseDto addVendorQuotation(
            @PathVariable Long procurementAssignmentId,
            @RequestBody AddVendorQuotationRequestDto requestDto
    ) {
        return procurementAssignmentService.addVendorQuotation(
                procurementAssignmentId,
                requestDto
        );
    }

    @PutMapping("/{procurementAssignmentId}/vendor-quotations/select")
    @Operation(summary = "Select final vendor quotation for procurement assignment")
    public ProcurementAssignmentResponseDto selectVendorQuotation(
            @PathVariable Long procurementAssignmentId,
            @RequestBody SelectVendorQuotationRequestDto requestDto
    ) {
        return procurementAssignmentService.selectVendorQuotation(
                procurementAssignmentId,
                requestDto
        );
    }

}