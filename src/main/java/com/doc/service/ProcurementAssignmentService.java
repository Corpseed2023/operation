package com.doc.service;

import com.doc.dto.procurement.ProcurementAssignmentResponseDto;
import com.doc.dto.procurement.SelectProcurementVendorRequestDto;

import java.util.List;

public interface ProcurementAssignmentService {

    ProcurementAssignmentResponseDto getProcurementAssignment(Long procurementAssignmentId);

    ProcurementAssignmentResponseDto getProcurementAssignmentByProject(Long projectId);

    List<ProcurementAssignmentResponseDto> getProcurementAssignmentsByUser(Long userId);

    ProcurementAssignmentResponseDto selectVendor(
            Long procurementAssignmentId,
            SelectProcurementVendorRequestDto requestDto
    );

    List<ProcurementAssignmentResponseDto> getVendorRequiredAssignments();
}