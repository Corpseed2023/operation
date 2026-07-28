package com.doc.dto.vendor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorMappedSolutionDto {

    private Long mappingId;
    private Long solutionId;
    private String solutionName;
    private Boolean active;
}