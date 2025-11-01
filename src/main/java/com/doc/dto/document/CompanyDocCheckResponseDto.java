package com.doc.dto.document;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CompanyDocCheckResponseDto {

    private boolean available;
    private String fileUrl;
    private String fileName;
    private LocalDate expiryDate;
    private String expiryStatus;
    private Integer daysUntilExpiry;
    private boolean isPermanent;
    private LocalDate verifiedDate;
    private boolean validationPassed;
    private String message;
}