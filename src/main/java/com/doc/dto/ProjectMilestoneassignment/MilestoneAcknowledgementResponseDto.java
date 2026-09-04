package com.doc.dto.ProjectMilestoneassignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneAcknowledgementResponseDto {

    private Long historyId;

    private Long milestoneAssignmentId;
    private Long milestoneId;
    private String milestoneName;
    private Integer milestoneOrder;

    private String acknowledgementAttachmentUrl;
    private String acknowledgementAttachmentName;

    private Long completedById;
    private String completedByName;
    private Date completedAt;

    private String completionReason;
}