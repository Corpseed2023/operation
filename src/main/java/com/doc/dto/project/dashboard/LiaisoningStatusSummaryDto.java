package com.doc.dto.project.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LiaisoningStatusSummaryDto {
    private String statusName;       // e.g. NEW, IN_PROGRESS, COMPLETED
    private long projectCount;
    private double totalAmount;      // sum of project.paymentDetail.totalAmount for projects in this status
}