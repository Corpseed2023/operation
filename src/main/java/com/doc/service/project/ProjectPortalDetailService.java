package com.doc.service.project;



import com.doc.dto.project.portal.*;
import com.doc.entity.project.ProjectPortalDetailStatus;

public interface ProjectPortalDetailService {

    ProjectPortalDetailResponseDto addPortalDetail(Long projectId, Long userId, ProjectPortalDetailRequestDto dto);

    ProjectPortalDetailListResponseDto getPortalDetails(Long projectId, Long userId);

    ProjectPortalDetailResponseDto updatePortalDetail(Long projectId, Long detailId, Long userId, ProjectPortalDetailRequestDto dto);

    void deletePortalDetail(Long projectId, Long detailId, Long userId);

    ProjectPortalDetailResponseDto approveOrRejectPortalDetail(Long projectId, Long detailId, Long userId, ProjectPortalDetailApprovalDto approvalDto);

    ProjectPortalApprovalQueueResponseDto getApprovalQueue(
            Long userId,
            ProjectPortalDetailStatus status,
            int page,
            int size
    );


}