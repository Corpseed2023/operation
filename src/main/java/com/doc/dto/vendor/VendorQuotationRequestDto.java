package com.doc.dto.vendor;

import com.doc.dto.vendor.request.VendorQuotationDocumentRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class VendorQuotationRequestDto {

    @NotNull(message = "RFQ id is required")
    private Long rfqId;

    @NotNull(message = "RFQ vendor id is required")
    private Long rfqVendorId;

    @NotNull(message = "Vendor id is required")
    private Long vendorId;

    private String quotationNumber;

    private Date quotationDate;

    private Date validFrom;

    private Date validTill;

    private String currency;

    private Integer deliveryDays;

    private String paymentTerms;

    private String warrantyTerms;

    private String remarks;

    @Valid
    private List<VendorQuotationDocumentRequestDto> documents;

    @NotNull(message = "Created by is required")
    private Long createdBy;

    @Valid
    @NotEmpty(message = "At least one quotation item is required")
    private List<VendorQuotationItemRequestDto> items;
}