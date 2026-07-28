package com.doc.dto.vendor;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRfqDashboardResponse {

    private Long rfqId;

    private String rfqNumber;

    private String title;

    private Date quotationSubmissionDeadline;

    private Long vendorsInvited;

    private Long quotationsReceived;

    private String status;
}
