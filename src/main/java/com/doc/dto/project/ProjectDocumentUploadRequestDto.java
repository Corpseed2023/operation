package com.doc.dto.project;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ProjectDocumentUploadRequestDto {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "Required document ID is required")
    private Long requiredDocumentId;

    @NotBlank(message = "File name cannot be empty")
    @Size(max = 255, message = "File name cannot exceed 255 characters")
    private String fileName;

    @NotBlank(message = "File URL cannot be empty")
    @Size(max = 1000, message = "File URL cannot exceed 1000 characters")
    private String fileUrl;

    @NotNull(message = "Uploaded by user ID is required")
    private Long uploadedById;

    @NotNull(message = "Created by user ID is required")
    private Long createdById;

    private Long companyDocSourceId;

    private Boolean isFromCompanyDoc = false;

    private Date expiryDate;

    private Boolean isPermanent = false;

    @Min(value = 1, message = "File size must be greater than 0")
    private Integer fileSizeKb;

    @NotBlank(message = "File format is required")
    @Pattern(regexp = "pdf|jpg|jpeg|png", message = "Only pdf, jpg, jpeg, png allowed")
    private String fileFormat;

    private String remarks;
}