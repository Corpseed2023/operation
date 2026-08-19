package com.doc.dto.company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyCompanyDocumentResponseDto {

    private Long id;

    private String documentType;

    private String fileUrl;
    private String fileName;
    private Integer fileSizeKb;
    private String fileFormat;

    private String documentNumber;
    private String remarks;

    private Long uploadedById;
    private String uploadedByName;
    private Date uploadTime;

    private Date createdDate;
    private Date updatedDate;
}