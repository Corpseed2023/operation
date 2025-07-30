package com.doc.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import com.doc.dto.contact.ContactRequestDto;

@Getter
@Setter
public class CompanyRequestDto {

    @NotBlank(message = "Company name cannot be empty")
    private String name;

    private String companyGstType;

    private String gstBusinessType;

    private String gstNo;

    private Date establishDate;

    private String industry;

    private String address;

    private String city;

    private String state;

    private String country;

    private String primaryPinCode;

    private List<ContactRequestDto> contacts;

    private String industries;

    private String subIndustry;

    private String subSubIndustry;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;
}