package com.doc.dto.company;

import com.doc.dto.company.unit.UnitSummary;      // ← correct import
import com.doc.dto.contact.ContactSummary;       // ← correct import
import lombok.*;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponseDto {

    private Long id;
    private String name;
    private String panNo;
    private Date establishDate;

    private String industry;
    // private String industries;   // ← removed if duplicate/not needed

    private String subIndustry;
    private String subSubIndustry;

    private Date createdDate;
    private Date updatedDate;
    private boolean deleted;

    private int unitCount;
    private List<UnitSummary> units;           // ← now resolves correctly

    private int contactCount;
    private List<ContactSummary> contacts;     // ← now resolves correctly
}