package com.doc.dto.project;

import com.doc.entity.project.DocumentStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

/**
 * DTO for returning project document details.
 */
@Getter
@Setter
public class DocumentResponseDto {
    private UUID id;
    private String fileUrl;
    private DocumentStatus status;
    private String remarks;
    private Date uploadTime;
    private UUID requiredDocumentId;
    private Long milestoneAssignmentId;
    private Long projectId;
    private Long uploadedById;
    private Date createdDate;
    private Date updatedDate;
}