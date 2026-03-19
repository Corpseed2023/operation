package com.doc.dto.company.unit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnitSummary {

    private Long unitId;
    private String unitName;
    private String city;
    private String gstNo;
}

