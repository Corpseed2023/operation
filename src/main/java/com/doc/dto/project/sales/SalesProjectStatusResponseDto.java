package com.doc.dto.project.sales;

import lombok.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesProjectStatusResponseDto {

    private Long projectId;
    private String projectName;
    private String projectNo;

    private Long productId;
    private String productName;

    private Long companyId;
    private String companyName;

    private Long unitId;
    private String unitName;

    private Long contactId;
    private String contactName;

    private String unbilledNumber;
    private String estimateNumber;

    private Long salesPersonId;
    private String salesPersonName;

    private Long projectStatusId;
    private String projectStatusName;

    private Double totalAmount;
    private Double dueAmount;
    private String paymentTypeName;

    private LocalDate projectDate;
    private Date createdDate;
    private Date updatedDate;

    private long totalMilestones;
    private long completedMilestones;
    private int milestoneCompletionPercentage;

    private List<DepartmentWiseMilestoneDto> departments;
}