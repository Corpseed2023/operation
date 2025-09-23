package com.doc.dto.project;

import com.doc.entity.project.DocumentStatus;
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
    private DocumentStatus status;
    private String remarks;
    private Date uploadTime;
    private Long requiredDocumentId;
    private Long milestoneAssignmentId;
    private Long projectId;
    private Long uploadedById;
    private Date createdDate;
    private Date updatedDate;
    private int replacementCount;
}