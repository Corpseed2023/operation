package com.doc.dto.company;

import com.doc.dto.company.unit.CompanyUnitRequestDto;
import com.doc.dto.contact.ContactRequestDto;
import lombok.*;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRequestDto {

    // Company basic info
    private String name;
    private String panNo;
    private Date establishDate;
    private String industry;
    private String industries;
    private String subIndustry;
    private String subSubIndustry;

    // Who is creating this company
    private Long createdBy;                 // Required - user ID

    // Optional: List of units to create along with company
    private List<CompanyUnitRequestDto> units;

    // Optional: List of contacts (company-level or unit-level)
    private List<ContactRequestDto> contacts;
}