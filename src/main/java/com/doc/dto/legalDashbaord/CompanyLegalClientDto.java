package com.doc.dto.legalDashbaord;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyLegalClientDto {
    private Long id;
    private Long companyId;
    private String companyName;
    private String documentType;
    private String status;
    private LocalDateTime createdAt;
}