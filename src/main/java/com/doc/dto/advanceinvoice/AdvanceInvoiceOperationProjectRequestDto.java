package com.doc.dto.advanceinvoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AdvanceInvoiceOperationProjectRequestDto {

    @NotNull(message = "estimateId is required")
    private Long estimateId;

    @NotBlank(message = "estimateNumber is required")
    private String estimateNumber;

    @NotNull(message = "invoiceId is required")
    private Long invoiceId;

    @NotBlank(message = "invoiceNumber is required")
    private String invoiceNumber;

    @NotNull(message = "companyId is required")
    private Long companyId;

    @NotNull(message = "unitId is required")
    private Long unitId;

    @NotNull(message = "contactId is required")
    private Long contactId;

    @NotNull(message = "solutionId is required")
    private Long solutionId;

    @NotBlank(message = "solutionName is required")
    private String solutionName;

    private Long leadId;

    @NotNull(message = "salesPersonId is required")
    private Long salesPersonId;

    private String salesPersonName;

    @NotNull(message = "createdBy is required")
    private Long createdBy;

    @NotNull(message = "approvedById is required")
    private Long approvedById;

    @NotNull(message = "totalAmount is required")
    private BigDecimal totalAmount;

    private BigDecimal receivedAmount;

    private BigDecimal outstandingAmount;

    private Long paymentTypeId;

    private LocalDate invoiceDate;
}