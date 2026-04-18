package com.doc.dto.LegalRequestDto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
public class LegalRequestDto {

    private Long id;
    private Long projectId;
    private Long milestoneId;
    private Double tatInDays;
    private String tatReason;
    private String status;

    private Long milestoneAssigneeId;
    private Long createdById;
    private Long updatedById;

    private Long assignedToLegal;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String statusReason;
    private String LegalRequestTitle;

    private String notes;

    private Long viewedBy;
    private LocalDateTime viewedAt;

    private List<String> documents;
}
