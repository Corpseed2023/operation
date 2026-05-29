package com.doc.dto.project;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProjectDocumentUploadRequestDto {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "Required document ID is required")
    private Long requiredDocumentId;

    @NotBlank(message = "File URL cannot be empty")
    @Size(max = 1000, message = "File URL cannot exceed 1000 characters")
    private String fileUrl;

    @NotBlank(message = "File name cannot be empty")
    @Size(max = 255, message = "File name cannot exceed 255 characters")
    private String fileName;

    @NotNull(message = "Uploaded by user ID is required")
    private Long uploadedById;

    @NotNull(message = "Created by user ID is required")
    private Long createdById;

    // Optional: used when document is reused from company documents
    private Long companyDocSourceId;

    private Boolean isFromCompanyDoc = false;

    // Optional: used for documents that expire
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    private Boolean isPermanent = false;

    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be greater than 0 KB")
    private Integer fileSizeKb;

    @NotBlank(message = "File format is required")
    @Pattern(
            regexp = "(?i)pdf|jpg|jpeg|png",
            message = "Only pdf, jpg, jpeg, png allowed"
    )
    private String fileFormat;

    @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
    private String remarks;


    @AssertTrue(message = "Permanent document should not have expiry date")
    public boolean isExpiryValidForPermanentDocument() {
        if (Boolean.TRUE.equals(isPermanent)) {
            return expiryDate == null;
        }
        return true;
    }
}