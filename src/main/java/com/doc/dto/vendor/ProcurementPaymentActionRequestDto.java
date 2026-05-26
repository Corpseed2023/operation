package com.doc.dto.vendor;

import lombok.Data;

import java.util.Date;

@Data
public class ProcurementPaymentActionRequestDto {

    private String comment;
    private String reason;

    private String invoiceNumber;
    private Date invoiceDate;
}