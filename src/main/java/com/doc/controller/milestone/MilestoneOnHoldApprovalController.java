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

    @GetMapping("/manager/{userId}")
    @Operation(
            summary = "Get milestone ON_HOLD requests",
            description = """
                    ADMIN and OPERATION_HEAD can see all requests.
                    A manager can see only requests assigned to them.
                    """
    )
    public ResponseEntity<Page<MilestoneOnHoldResponseDto>> getManagerQueue(
            @PathVariable Long userId,
            @RequestParam(required = false)
            MilestoneOnHoldApprovalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number must be greater than or equal to zero"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                approvalService.getManagerQueue(
                        userId,
                        status,
                        pageable
                )
        );
    }

    @PutMapping("/{requestId}/decision")
    @Operation(summary = "Approve or reject a milestone ON_HOLD request")
    public ResponseEntity<MilestoneOnHoldResponseDto> decide(
            @PathVariable Long requestId,
            @Valid @RequestBody MilestoneOnHoldDecisionDto dto
    ) {
        return ResponseEntity.ok(
                approvalService.decide(requestId, dto)
        );
    }
}