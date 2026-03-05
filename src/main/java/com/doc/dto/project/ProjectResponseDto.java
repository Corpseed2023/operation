package com.doc.dto.project;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
public class ProjectResponseDto {

    private Long id;
    private String name;
    private String projectNo;
    private String unbilledNumber;
    private String estimateNumber;

    // Sales Person
    private Long salesPersonId;
    private String salesPersonName;          // NEW

    private Long productId;
    private String productName;
    private Long companyId;
    private String companyName;              // NEW

    private Long contactId;
    private String contactName;              // NEW

    private Long leadId;
    private LocalDate date;
    private Double totalAmount;
    private Double dueAmount;
    private String paymentStatus;
    private Long paymentTypeId;
    private Long approvedById;
    private Long createdById;

    private Date createdDate;
    private Date updatedDate;

    private boolean isDeleted;
    private boolean isActive;

    private Long statusId;
    private String statusName;

    // Assume this class exists – add:
    private Long unitId;
    private String unitName;
}