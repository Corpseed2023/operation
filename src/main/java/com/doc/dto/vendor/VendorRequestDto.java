package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorRequestDto {

    @NotBlank(message = "Vendor code is required")
    @Size(max = 50)
    private String vendorCode;

    @NotBlank(message = "Vendor name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String mobile;

    @Size(max = 15)
    private String gstNumber;

    @Size(max = 10)
    private String panNumber;

    private VendorStatus status;

    private boolean isVerified = false;

    @NotNull(message = "CreatedBy user ID is required")
    private Long createdBy;

    private Long updatedBy;
}