package com.doc.dto.project;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProjectRequestDto {

    @NotBlank(message = "Project name cannot be empty")
    private String name;

    @NotBlank(message = "Project number cannot be empty")
    private String projectNo;

    @NotNull(message = "Sales person ID cannot be null")
    private Long salesPersonId;

    @NotNull(message = "Product ID cannot be null")
    private Long productId;

    @NotNull(message = "Company ID cannot be null")
    private Long companyId;

    @NotNull(message = "Contact ID cannot be null")
    private Long contactId;

    private Long leadId;

    private LocalDate date;

    private String address;

    private String city;

    private String state;

    private String country;

    private String primaryPinCode;

    @NotNull(message = "Total amount cannot be null")
    private Double totalAmount;

    @NotNull(message = "paid amount cannot be null")
    private Double paidAmount;

    @NotBlank(message = "Payment status cannot be empty")
    private String paymentStatus;

    @NotNull(message = "Payment type ID cannot be null")
    private Long paymentTypeId;

    @NotNull(message = "Approved by user ID cannot be null")
    private Long approvedById;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;

    @NotNull(message = "Updated by user ID cannot be null")
    private Long updatedBy;
}
