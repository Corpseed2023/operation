package com.doc.dto.company.unit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class CompanyUnitRequestDto {

    private Long   unitId;
    private String unitName;                // Required
    private String address;                 // Required
    private String city;
    private String state;
    private String country = "India";
    private String pinCode;
    private String gstNo;
    private String gstType;
    private String gstBusinessType;
    private Date unitOpeningDate;
}