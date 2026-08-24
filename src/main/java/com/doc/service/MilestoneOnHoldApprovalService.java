package com.doc.service;

import com.doc.dto.ProjectMilestoneassignment.MilestoneOnHoldDecisionDto;
import com.doc.dto.ProjectMilestoneassignment.MilestoneOnHoldResponseDto;
import com.doc.dto.ProjectMilestoneassignment.UpdateMilestoneStatusDto;
import com.doc.entity.milestone.MilestoneOnHoldApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MilestoneOnHoldApprovalService {

    MilestoneOnHoldResponseDto requestOnHold(UpdateMilestoneStatusDto dto);

    MilestoneOnHoldResponseDto decide(
            Long requestId,
            MilestoneOnHoldDecisionDto dto
    );

    Page<MilestoneOnHoldResponseDto> getManagerQueue(
            Long managerId,
            MilestoneOnHoldApprovalStatus approvalStatus,
            Pageable pageable
    );

    boolean hasPendingRequest(Long assignmentId);
}
