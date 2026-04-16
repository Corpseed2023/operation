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
    private String unitName;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pinCode;
    private String gstNo;
    private Date unitOpeningDate;
}