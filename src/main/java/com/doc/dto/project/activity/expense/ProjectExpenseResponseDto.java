package com.doc.dto.project.activity.expense;

import com.doc.em.ApprovalStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProjectExpenseResponseDto {

    private Long expenseId;
    private Long activityId;

    // Expense fields
    private String expenseType;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime expenseDate;
    private String paymentMedium;
    private ApprovalStatus approvalStatus;
    private boolean isApproved;
    private Long approvedByUserId;
    private String approvedByUserName;
    private Long createdByUserId;
    private String createdByUserName;
    private LocalDateTime createdDate;

    private Long projectId;
    private String projectNo;
    private String projectName;
    private String unbilledNumber;
    private String productName;
}