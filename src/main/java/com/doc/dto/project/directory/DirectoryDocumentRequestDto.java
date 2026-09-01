package com.doc.dto.project.directory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DirectoryDocumentRequestDto {

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name cannot exceed 255 characters")
    private String fileName;

    @NotBlank(message = "File URL is required")
    @Size(max = 1000, message = "File URL cannot exceed 1000 characters")
    private String fileUrl;

    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be greater than zero")
    private Integer fileSizeKb;

    @NotBlank(message = "File format is required")
    @Pattern(
            regexp = "(?i)pdf|jpg|jpeg|png",
            message = "Only pdf, jpg, jpeg and png are allowed"
    )
    private String fileFormat;

    @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
    private String remarks;
}