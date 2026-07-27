package com.doc.dto.vendor;
import com.doc.entity.vendor.VendorRestrictionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorRestrictionRequestDto {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotNull(message = "Restriction type is required")
    private VendorRestrictionType restrictionType;

    @NotBlank(message = "Restriction reason is required")
    @Size(
            max = 2000,
            message = "Restriction reason cannot exceed 2000 characters"
    )
    private String reason;

    /*
     * Required only when restrictionType = SUSPENSION.
     */
    private LocalDate restrictionStartDate;

    /*
     * Required only when restrictionType = SUSPENSION.
     */
    private LocalDate restrictionEndDate;

    /*
     * Optional supporting document.
     */
    @Size(
            max = 1000,
            message = "Attachment URL cannot exceed 1000 characters"
    )
    private String attachmentUrl;
}