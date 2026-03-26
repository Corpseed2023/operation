package com.doc.dto.project.activity.expense;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ApproveExpenseRequestDto {

    private String status;
    private String rejectionRemark;
}