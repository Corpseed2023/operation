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
    private Long salesPersonId;
    private Long productId;
    private Long companyId;
    private Long contactId;
    private Long leadId;
    private LocalDate date;
    private String address;
    private String city;
    private String state;
    private String country;
    private String primaryPinCode;
    private Double totalAmount;
    private Double dueAmount;
    private String paymentStatus;
    private Long paymentTypeId;
    private Long approvedById;
    private Date createdDate;
    private Date updatedDate;
    private boolean isDeleted;
    private boolean isActive;
}
