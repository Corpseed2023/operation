package com.doc.dto.legalDashbaord;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentLegalClientDto {
    private Long id;
    private Long unbilledInvoiceId;
    private String unbilledNumber;
    private String companyName;
    private String status;
    private LocalDateTime createdAt;
}