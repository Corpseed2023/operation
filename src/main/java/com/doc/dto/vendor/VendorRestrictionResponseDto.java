package com.doc.dto.vendor;

import com.doc.entity.vendor.VendorRestrictionRequestStatus;
import com.doc.entity.vendor.VendorRestrictionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorRestrictionResponseDto {

    private Long id;

    private Long vendorId;

    private String vendorName;

    private VendorRestrictionType restrictionType;

    private VendorRestrictionRequestStatus status;

    private String reason;

    private LocalDate restrictionStartDate;

    private LocalDate restrictionEndDate;

    private String attachmentUrl;

    /*
     * Request details
     */
    private Long requestedBy;

    private String requestedByName;

    private LocalDateTime requestedAt;

    /*
     * Accounts review details
     */
    private Long accountsReviewedBy;

    private String accountsReviewedByName;

    private LocalDateTime accountsReviewedAt;

    private String accountsRemarks;

    /*
     * Admin review details
     */
    private Long adminReviewedBy;

    private String adminReviewedByName;

    private LocalDateTime adminReviewedAt;

    private String adminRemarks;
}
