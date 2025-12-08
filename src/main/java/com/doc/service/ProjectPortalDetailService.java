package com.doc.service;



import com.doc.dto.project.portal.ProjectPortalDetailApprovalDto;
import com.doc.dto.project.portal.ProjectPortalDetailListResponseDto;
import com.doc.dto.project.portal.ProjectPortalDetailRequestDto;
import com.doc.dto.project.portal.ProjectPortalDetailResponseDto;

public interface ProjectPortalDetailService {

    ProjectPortalDetailResponseDto addPortalDetail(Long projectId, Long userId, ProjectPortalDetailRequestDto dto);

    ProjectPortalDetailListResponseDto getPortalDetails(Long projectId, Long userId);

    ProjectPortalDetailResponseDto updatePortalDetail(Long projectId, Long detailId, Long userId, ProjectPortalDetailRequestDto dto);

    void deletePortalDetail(Long projectId, Long detailId, Long userId);

    ProjectPortalDetailResponseDto approveOrRejectPortalDetail(Long projectId, Long detailId, Long userId, ProjectPortalDetailApprovalDto approvalDto);
}