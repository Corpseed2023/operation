package com.doc.dto.vendor;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RFQCreateRequestDto {

    @NotBlank(message = "RFQ title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Size(max = 1000, message = "Scope of work cannot exceed 1000 characters")
    private String scopeOfWork;

    @Size(max = 1000, message = "Terms and conditions cannot exceed 1000 characters")
    private String termsAndConditions;

    @Size(max = 500, message = "Delivery location cannot exceed 500 characters")
    private String deliveryLocation;

    @NotNull(message = "Quotation submission deadline is required")
    private Date quotationSubmissionDeadline;

    private Date expectedStartDate;

    private Date expectedEndDate;

    @Size(max = 255, message = "Contact person name cannot exceed 255 characters")
    private String contactPersonName;

    @Email(message = "Invalid contact person email")
    @Size(max = 255, message = "Contact person email cannot exceed 255 characters")
    private String contactPersonEmail;

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Contact person mobile must be 10 digits"
    )
    private String contactPersonMobile;

    @Size(max = 500, message = "Attachment URL cannot exceed 500 characters")
    private String attachmentUrl;

    @NotEmpty(message = "At least one vendor is required")
    private List<Long> vendorIds;
}