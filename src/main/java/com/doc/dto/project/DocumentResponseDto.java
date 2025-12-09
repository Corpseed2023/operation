// src/main/java/com/doc/dto/project/DocumentResponseDto.java

package com.doc.dto.project;

import com.doc.entity.document.DocumentStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentResponseDto {

    private Long id;
    private String fileUrl;
    private String fileName;
    private String oldFileUrl;
    private String oldFileName;
//    private DocumentStatus status;
    private String status;        // e.g., "VERIFIED", "UPLOADED", "REJECTED"
    private String remarks;
    private Date uploadTime;
    private Date expiryDate;
    private boolean isPermanent;
    private boolean isExpired;
    private Integer fileSizeKb;
    private String fileFormat;
    private boolean validationPassed;
    private String validationIssues;
    private Long requiredDocumentId;
    private Long milestoneAssignmentId;
    private Long projectId;
    private Long uploadedById;
    private Long createdBy;
    private Long updatedBy;
    private Date createdDate;
    private Date updatedDate;
    private int replacementCount;
    private boolean isFromCompanyDoc;
    private Long companyDocSourceId;
}