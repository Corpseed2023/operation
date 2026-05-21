package com.doc.dto.vendor;

import com.doc.entity.vendor.POStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
public class PurchaseOrderResponseDto {

    private Long id;
    private String poNumber;
    private Long procurementAssignmentId;
    private Long projectId;
    private Long vendorId;
    private String vendorName;
    private BigDecimal totalAmount;
    private BigDecimal gstAmount;
    private BigDecimal grandTotal;
    private String scopeOfWork;
    private LocalDate issueDate;
    private LocalDate validTillDate;
    private POStatus status;
    private String paymentTypeName;
    private String termsAndConditions;
    private Date createdDate;
    private Date approvedDate;
}