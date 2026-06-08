package com.doc.dto.LegalRequestDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LegalRequestResponseDto {

    private Long id;

    private Long projectId;

    private Long projectMilestoneAssignmentId;

    private Long milestoneAssigneeId;

    private Long assignedToLegal;

    private String legalRequestTitle;

    private String status;

    private String notes;

    private String statusReason;

    private String resolutionSummary;

    private Long createdById;

    private Long updatedById;

    private Long viewedBy;

    private LocalDateTime viewedAt;

    private Long resolvedBy;

    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<LegalRequestDocumentResponseDto> documents = new ArrayList<>();
}