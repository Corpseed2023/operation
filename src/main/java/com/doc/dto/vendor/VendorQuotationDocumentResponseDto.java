package com.doc.dto.vendor;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class VendorQuotationDocumentResponseDto {

    private Long id;

    private Long quotationId;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Long fileSizeKb;

    private Long createdBy;

    private Date createdDate;

    private boolean deleted;
}