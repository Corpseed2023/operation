package com.doc.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVendorSyncRequestDto {

    /*
     * Vendor ID from Operation Service.
     */
    private Long operationVendorId;

    /*
     * Approved vendor accounts submission ID.
     */
    private Long vendorAccountsSubmissionId;

    /*
     * Vendor finalization ID.
     */
    private Long vendorFinalizationId;

    /*
     * Vendor master information.
     */
    private String vendorName;

    private String email;

    private String mobile;

    private String pan;

    private String gstNumber;

    private String gstRegistrationType;

    /*
     * Vendor bank information.
     */
    private String accountHolderName;

    private String bankAccountNumber;

    private String ifscCode;

    private String bankName;

    private String branchAddress;

    /*
     * Vendor address information.
     */
    private String fullAddress;

    private String city;

    private String state;

    private String country;

    /*
     * Vendor active status in Operation Service.
     */
    private Boolean active;

    /*
     * Operation Service user who approved the vendor
     * or procurement payment request.
     */
    private Long approvedByOperationUserId;

    /*
     * Approval timestamp from Operation Service.
     */
    private LocalDateTime approvedAt;

    /*
     * Latest vendor update timestamp in Operation Service.
     */
    private LocalDateTime operationUpdatedAt;

    /*
     * Null during normal vendor onboarding.
     *
     * Populated only when a procurement vendor payment
     * request is approved.
     *
     * Operation Service sends only:
     * - price
     * - GST applicability and percentage
     * - GST type/state
     * - TDS applicability and percentage
     *
     * Account Service calculates:
     * - CGST/SGST/IGST
     * - total GST
     * - TDS amount
     * - net payment amount
     *
     * Account Service then creates:
     * - AccountingVoucher
     * - AccountingVoucherEntry
     * - required system ledger entries
     */
    private VendorPaymentApprovalRequestDto paymentApproval;
}