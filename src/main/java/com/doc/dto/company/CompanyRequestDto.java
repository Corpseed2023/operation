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

    private String name;
    private String panNo;
    private String industry;
    private String industries;
    private String subIndustry;
    private String subSubIndustry;

    private Long createdBy;

    private List<CompanyUnitRequestDto> units;

    private List<ContactRequestDto> contacts;
}