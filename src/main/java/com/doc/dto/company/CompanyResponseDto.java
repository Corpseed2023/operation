package com.doc.dto.company;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class CompanyResponseDto {

    private Long id;

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

    private List<Long> contactIds;

    private String industries;

    private String subIndustry;

    private String subSubIndustry;

    private Date createdDate;

    private Date updatedDate;
}