package com.doc.controller.milestone;

import com.doc.dto.ProjectMilestoneassignment.MilestoneOnHoldDecisionDto;
import com.doc.dto.ProjectMilestoneassignment.MilestoneOnHoldResponseDto;
import com.doc.entity.milestone.MilestoneOnHoldApprovalStatus;
import com.doc.service.MilestoneOnHoldApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/milestone-on-hold-requests")
public class MilestoneOnHoldApprovalController {

    private final MilestoneOnHoldApprovalService approvalService;

    public MilestoneOnHoldApprovalController(
            MilestoneOnHoldApprovalService approvalService
    ) {
        this.approvalService = approvalService;
    }

    @GetMapping("/manager/{managerId}")
    @Operation(summary = "Get milestone ON_HOLD requests assigned to a manager")
    public ResponseEntity<Page<MilestoneOnHoldResponseDto>> getManagerQueue(
            @PathVariable Long managerId,
            @RequestParam(required = false) MilestoneOnHoldApprovalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                approvalService.getManagerQueue(managerId, status, pageable)
        );
    }

    @PutMapping("/{requestId}/decision")
    @Operation(summary = "Approve or reject a milestone ON_HOLD request")
    public ResponseEntity<MilestoneOnHoldResponseDto> decide(
            @PathVariable Long requestId,
            @Valid @RequestBody MilestoneOnHoldDecisionDto dto
    ) {
        return ResponseEntity.ok(approvalService.decide(requestId, dto));
    }
}
