package com.doc.impl.vendor;
import com.doc.dto.vendor.*;
import com.doc.entity.vendor.*;
import com.doc.dto.vendor.RFQVendorResponseDto;
import com.doc.dto.vendor.VendorRequestDto;
import com.doc.dto.vendor.VendorResponseDto;
import com.doc.entity.user.User;
import com.doc.entity.vendor.Vendor;
import com.doc.entity.vendor.VendorStatus;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.UserRepository;
import com.doc.repository.vendor.*;
import com.doc.service.vendor.VendorMailService;
import com.doc.service.vendor.VendorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VendorServiceImpl implements VendorService {

    private static final Logger logger = LoggerFactory.getLogger(VendorServiceImpl.class);

    private final VendorRepository vendorRepository;
    private final VendorMailService vendorMailService;
    private final UserRepository userRepository;

    private final RFQVendorRepository rfqVendorRepository;
    private final VendorQuotationRepository vendorQuotationRepository;
    private final VendorFinalizationRepository vendorFinalizationRepository;
    private final VendorOnboardingRepository vendorOnboardingRepository;
    private final VendorRestrictionRequestRepository vendorRestrictionRequestRepository;

    public VendorServiceImpl(
            VendorRepository vendorRepository,
            VendorMailService vendorMailService,
            UserRepository userRepository,
            RFQVendorRepository rfqVendorRepository,
            VendorQuotationRepository vendorQuotationRepository,
            VendorFinalizationRepository vendorFinalizationRepository,
            VendorOnboardingRepository vendorOnboardingRepository,
            VendorRestrictionRequestRepository vendorRestrictionRequestRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorMailService = vendorMailService;
        this.userRepository = userRepository;
        this.rfqVendorRepository = rfqVendorRepository;
        this.vendorQuotationRepository = vendorQuotationRepository;
        this.vendorFinalizationRepository = vendorFinalizationRepository;
        this.vendorOnboardingRepository = vendorOnboardingRepository;
        this.vendorRestrictionRequestRepository = vendorRestrictionRequestRepository;

    }

    @Override
    @Transactional
    public VendorResponseDto createVendor(Long userId, VendorRequestDto dto) {

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ValidationException(
                    "Vendor name is required",
                    "ERR_VENDOR_NAME_REQUIRED"
            );
        }

        VendorGSTRegistrationType gstRegistrationType =
                dto.getGstRegistrationType();

        String gstNumber = normalize(dto.getGstNumber());
        String panNumber = normalize(dto.getPanNumber());

        boolean gstNumberRequired =
                gstRegistrationType == VendorGSTRegistrationType.REGISTERED ||
                        gstRegistrationType == VendorGSTRegistrationType.SEZ;

        if (gstNumberRequired && gstNumber == null) {
            throw new ValidationException(
                    "GST number is mandatory for Registered and SEZ vendors",
                    "ERR_GST_NUMBER_REQUIRED"
            );
        }

        if (gstNumber != null && gstNumber.length() != 15) {
            throw new ValidationException(
                    "GST number must contain exactly 15 characters",
                    "ERR_INVALID_GST_NUMBER"
            );
        }

        if (gstNumber != null &&
                vendorRepository.existsByGstNumberAndIsDeletedFalse(gstNumber)) {
            throw new ValidationException(
                    "GST number already exists",
                    "ERR_DUPLICATE_GST"
            );
        }

        if (panNumber != null &&
                vendorRepository.existsByPanNumberAndIsDeletedFalse(panNumber)) {
            throw new ValidationException(
                    "PAN number already exists",
                    "ERR_DUPLICATE_PAN"
            );
        }

        User createdByUser = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CreatedBy user not found",
                        "ERR_USER_NOT_FOUND"
                ));

        Vendor vendor = new Vendor();

        vendor.setName(dto.getName().trim());
        vendor.setDescription(dto.getDescription());
        vendor.setEmail(normalize(dto.getEmail()));
        vendor.setMobile(normalize(dto.getMobile()));

        vendor.setGstRegistrationType(gstRegistrationType);
        vendor.setGstNumber(gstNumber);

        vendor.setPanNumber(panNumber);
        vendor.setStatus(VendorStatus.PROSPECTIVE);
        vendor.setCreatedBy(createdByUser.getId());
        vendor.setUpdatedBy(createdByUser.getId());
        vendor.setDeleted(false);

        vendor = vendorRepository.save(vendor);

        logger.info(
                "Vendor created successfully with ID: {}",
                vendor.getId()
        );

        return mapEntityToDto(vendor);
    }

    @Override
    @Transactional
    public VendorRestrictionResponseDto restrictVendor(
            Long vendorId,
            Long userId,
            VendorRestrictionRequestDto dto) {

        if (vendorId == null) {
            throw new ValidationException(
                    "Vendor ID is required",
                    "ERR_VENDOR_ID_REQUIRED"
            );
        }

        if (userId == null) {
            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        if (dto == null) {
            throw new ValidationException(
                    "Vendor restriction request is required",
                    "ERR_VENDOR_RESTRICTION_REQUEST_REQUIRED"
            );
        }

        /*
         * Because vendorId is present in both path variable and request body,
         * validate that both values are the same.
         */
        if (dto.getVendorId() != null
                && !vendorId.equals(dto.getVendorId())) {

            throw new ValidationException(
                    "Vendor ID in request body does not match path vendor ID",
                    "ERR_VENDOR_ID_MISMATCH"
            );
        }

        /*
         * Validate requesting user.
         */
        User requestedByUser = userRepository
                .findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_ACTIVE_USER_NOT_FOUND"
                ));

        /*
         * Fetch vendor.
         */
        Vendor vendor = vendorRepository
                .findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor not found with ID: " + vendorId,
                        "ERR_VENDOR_NOT_FOUND"
                ));

        /*
         * Deleted vendors cannot be restricted.
         */
        if ((vendor.isDeleted())) {
            throw new ValidationException(
                    "Deleted vendor cannot be suspended or blacklisted",
                    "ERR_VENDOR_DELETED"
            );
        }

        /*
         * Only ACTIVE vendors can enter this workflow.
         */
        if (vendor.getStatus() != VendorStatus.ACTIVE) {
            throw new ValidationException(
                    "Only an ACTIVE vendor can be suspended or blacklisted. "
                            + "Current status: " + vendor.getStatus(),
                    "ERR_VENDOR_NOT_ACTIVE"
            );
        }

        if (dto.getRestrictionType() == null) {
            throw new ValidationException(
                    "Restriction type is required",
                    "ERR_RESTRICTION_TYPE_REQUIRED"
            );
        }

        if (dto.getReason() == null
                || dto.getReason().trim().isEmpty()) {

            throw new ValidationException(
                    "Restriction reason is required",
                    "ERR_RESTRICTION_REASON_REQUIRED"
            );
        }

        /*
         * Suspension requires start and end dates.
         */
        if (dto.getRestrictionType()
                == VendorRestrictionType.SUSPENSION) {

            if (dto.getRestrictionStartDate() == null) {
                throw new ValidationException(
                        "Restriction start date is required for suspension",
                        "ERR_RESTRICTION_START_DATE_REQUIRED"
                );
            }

            if (dto.getRestrictionEndDate() == null) {
                throw new ValidationException(
                        "Restriction end date is required for suspension",
                        "ERR_RESTRICTION_END_DATE_REQUIRED"
                );
            }

            if (dto.getRestrictionStartDate()
                    .isBefore(LocalDate.now())) {

                throw new ValidationException(
                        "Restriction start date cannot be before today",
                        "ERR_RESTRICTION_START_DATE_IN_PAST"
                );
            }

            if (dto.getRestrictionEndDate()
                    .isBefore(dto.getRestrictionStartDate())) {

                throw new ValidationException(
                        "Restriction end date cannot be before start date",
                        "ERR_INVALID_RESTRICTION_DATE_RANGE"
                );
            }
        }

        /*
         * Blacklist does not require start or end dates.
         */
        if (dto.getRestrictionType()
                == VendorRestrictionType.BLACKLIST) {

            if (dto.getRestrictionStartDate() != null
                    || dto.getRestrictionEndDate() != null) {

                throw new ValidationException(
                        "Restriction dates are not allowed for blacklist requests",
                        "ERR_BLACKLIST_DATES_NOT_ALLOWED"
                );
            }
        }

        /*
         * Prevent multiple pending requests for the same vendor.
         */
        boolean pendingRequestExists =
                vendorRestrictionRequestRepository
                        .existsByVendor_IdAndStatusIn(
                                vendorId,
                                List.of(
                                        VendorRestrictionRequestStatus
                                                .PENDING_ACCOUNTS,

                                        VendorRestrictionRequestStatus
                                                .PENDING_ADMIN
                                )
                        );

        if (pendingRequestExists) {
            throw new ValidationException(
                    "A pending restriction request already exists for this vendor",
                    "ERR_PENDING_VENDOR_RESTRICTION_EXISTS"
            );
        }

        /*
         * Create request only.
         *
         * Vendor status must not be changed here.
         * Vendor will be updated only after final Admin approval.
         */
        VendorRestrictionRequest restrictionRequest =
                VendorRestrictionRequest.builder()
                        .vendor(vendor)
                        .restrictionType(dto.getRestrictionType())
                        .status(
                                VendorRestrictionRequestStatus
                                        .PENDING_ACCOUNTS
                        )
                        .reason(dto.getReason().trim())
                        .restrictionStartDate(
                                dto.getRestrictionStartDate()
                        )
                        .restrictionEndDate(
                                dto.getRestrictionEndDate()
                        )
                        .attachmentUrl(
                                normalizeNullableText(
                                        dto.getAttachmentUrl()
                                )
                        )
                        .requestedBy(userId)
                        .requestedAt(LocalDateTime.now())
                        .build();

        VendorRestrictionRequest savedRequest =
                vendorRestrictionRequestRepository.save(
                        restrictionRequest
                );

        return VendorRestrictionResponseDto.builder()
                .id(savedRequest.getId())
                .vendorId(vendor.getId())
                .vendorName(vendor.getName())
                .restrictionType(savedRequest.getRestrictionType())
                .status(savedRequest.getStatus())
                .reason(savedRequest.getReason())
                .restrictionStartDate(
                        savedRequest.getRestrictionStartDate()
                )
                .restrictionEndDate(
                        savedRequest.getRestrictionEndDate()
                )
                .attachmentUrl(savedRequest.getAttachmentUrl())
                .requestedBy(savedRequest.getRequestedBy())
                .requestedByName(requestedByUser.getFullName())
                .requestedAt(savedRequest.getRequestedAt())
                .build();
    }

    @Override
    @Transactional
    public VendorRestrictionResponseDto reviewRestrictionByAccounts(
            Long requestId,
            Long userId,
            VendorRestrictionAccountsReviewDto dto) {

        if (requestId == null) {
            throw new ValidationException(
                    "Restriction request ID is required",
                    "ERR_RESTRICTION_REQUEST_ID_REQUIRED"
            );
        }

        if (userId == null) {
            throw new ValidationException(
                    "Accounts user ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        if (dto == null) {
            throw new ValidationException(
                    "Accounts review request is required",
                    "ERR_ACCOUNTS_REVIEW_REQUEST_REQUIRED"
            );
        }

        if (dto.getApproved() == null) {
            throw new ValidationException(
                    "Approval status is required",
                    "ERR_APPROVAL_STATUS_REQUIRED"
            );
        }

        /*
         * Validate the Accounts user.
         */
        User accountsUser = userRepository
                .findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active Accounts user not found with ID: " + userId,
                        "ERR_ACTIVE_USER_NOT_FOUND"
                ));

        /*
         * Add Accounts department or role validation here.
         *
         * Example:
         *
         * boolean accountsUserValid =
         *         userRepository.existsActiveUserInDepartment(
         *                 userId,
         *                 ACCOUNTS_DEPARTMENT_ID
         *         );
         *
         * if (!accountsUserValid) {
         *     throw new ValidationException(
         *             "User does not belong to the Accounts department",
         *             "ERR_USER_NOT_IN_ACCOUNTS"
         *     );
         * }
         */

        /*
         * Lock and fetch the request.
         */
        VendorRestrictionRequest restrictionRequest =
                vendorRestrictionRequestRepository
                        .findByIdForUpdate(requestId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Vendor restriction request not found with ID: "
                                        + requestId,
                                "ERR_VENDOR_RESTRICTION_REQUEST_NOT_FOUND"
                        ));

        /*
         * Accounts can review only PENDING_ACCOUNTS requests.
         */
        if (restrictionRequest.getStatus()
                != VendorRestrictionRequestStatus.PENDING_ACCOUNTS) {

            throw new ValidationException(
                    "Restriction request is not pending for Accounts review. "
                            + "Current status: "
                            + restrictionRequest.getStatus(),
                    "ERR_REQUEST_NOT_PENDING_ACCOUNTS"
            );
        }

        /*
         * Rejection remarks are mandatory.
         */
        if (Boolean.FALSE.equals(dto.getApproved())
                && (dto.getRemarks() == null
                || dto.getRemarks().trim().isEmpty())) {

            throw new ValidationException(
                    "Accounts remarks are required when rejecting the request",
                    "ERR_ACCOUNTS_REJECTION_REMARKS_REQUIRED"
            );
        }

        restrictionRequest.setAccountsReviewedBy(userId);
        restrictionRequest.setAccountsReviewedAt(LocalDateTime.now());
        restrictionRequest.setAccountsRemarks(
                normalizeNullableText(dto.getRemarks())
        );

        if (Boolean.TRUE.equals(dto.getApproved())) {

            /*
             * Send approved request to Admin.
             */
            restrictionRequest.setStatus(
                    VendorRestrictionRequestStatus.PENDING_ADMIN
            );

        } else {

            /*
             * End workflow after Accounts rejection.
             */
            restrictionRequest.setStatus(
                    VendorRestrictionRequestStatus.ACCOUNTS_REJECTED
            );
        }

        VendorRestrictionRequest savedRequest =
                vendorRestrictionRequestRepository.save(
                        restrictionRequest
                );

        String requestedByName = userRepository
                .findById(savedRequest.getRequestedBy())
                .map(User::getFullName)
                .orElse(null);

        Vendor vendor = savedRequest.getVendor();

        return VendorRestrictionResponseDto.builder()
                .id(savedRequest.getId())
                .vendorId(vendor.getId())
                .vendorName(vendor.getName())
                .restrictionType(savedRequest.getRestrictionType())
                .status(savedRequest.getStatus())
                .reason(savedRequest.getReason())
                .restrictionStartDate(
                        savedRequest.getRestrictionStartDate()
                )
                .restrictionEndDate(
                        savedRequest.getRestrictionEndDate()
                )
                .attachmentUrl(savedRequest.getAttachmentUrl())
                .requestedBy(savedRequest.getRequestedBy())
                .requestedByName(requestedByName)
                .requestedAt(savedRequest.getRequestedAt())
                .accountsReviewedBy(
                        savedRequest.getAccountsReviewedBy()
                )
                .accountsReviewedByName(
                        accountsUser.getFullName()
                )
                .accountsReviewedAt(
                        savedRequest.getAccountsReviewedAt()
                )
                .accountsRemarks(
                        savedRequest.getAccountsRemarks()
                )
                .build();
    }

    @Override
    @Transactional
    public VendorRestrictionResponseDto reviewRestrictionByAdmin(
            Long requestId,
            Long userId,
            VendorRestrictionAdminReviewDto dto) {

        if (requestId == null) {
            throw new ValidationException(
                    "Restriction request ID is required",
                    "ERR_RESTRICTION_REQUEST_ID_REQUIRED"
            );
        }

        if (userId == null) {
            throw new ValidationException(
                    "Admin user ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        if (dto == null) {
            throw new ValidationException(
                    "Admin review request is required",
                    "ERR_ADMIN_REVIEW_REQUEST_REQUIRED"
            );
        }

        if (dto.getApproved() == null) {
            throw new ValidationException(
                    "Approval status is required",
                    "ERR_APPROVAL_STATUS_REQUIRED"
            );
        }

        /*
         * Validate active Admin user.
         */
        User adminUser = userRepository
                .findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active Admin user not found with ID: " + userId,
                        "ERR_ACTIVE_USER_NOT_FOUND"
                ));

        /*
         * Add Admin department or role validation here.
         *
         * Example:
         *
         * boolean validAdmin =
         *         userRepository.existsActiveUserInDepartment(
         *                 userId,
         *                 ADMIN_DEPARTMENT_ID
         *         );
         *
         * if (!validAdmin) {
         *     throw new ValidationException(
         *             "User does not belong to the Admin department",
         *             "ERR_USER_NOT_IN_ADMIN"
         *     );
         * }
         */

        /*
         * Lock and fetch the request.
         */
        VendorRestrictionRequest restrictionRequest =
                vendorRestrictionRequestRepository
                        .findByIdForUpdate(requestId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Vendor restriction request not found with ID: "
                                        + requestId,
                                "ERR_VENDOR_RESTRICTION_REQUEST_NOT_FOUND"
                        ));

        /*
         * Admin can review only after Accounts approval.
         */
        if (restrictionRequest.getStatus()
                != VendorRestrictionRequestStatus.PENDING_ADMIN) {

            throw new ValidationException(
                    "Restriction request is not pending for Admin review. "
                            + "Current status: "
                            + restrictionRequest.getStatus(),
                    "ERR_REQUEST_NOT_PENDING_ADMIN"
            );
        }

        /*
         * Remarks are mandatory for rejection.
         */
        if (Boolean.FALSE.equals(dto.getApproved())
                && (dto.getRemarks() == null
                || dto.getRemarks().trim().isEmpty())) {

            throw new ValidationException(
                    "Admin remarks are required when rejecting the request",
                    "ERR_ADMIN_REJECTION_REMARKS_REQUIRED"
            );
        }

        LocalDateTime reviewTime = LocalDateTime.now();

        restrictionRequest.setAdminReviewedBy(userId);
        restrictionRequest.setAdminReviewedAt(reviewTime);
        restrictionRequest.setAdminRemarks(
                normalizeNullableText(dto.getRemarks())
        );

        Vendor vendor = restrictionRequest.getVendor();

        if (Boolean.TRUE.equals(dto.getApproved())) {

            /*
             * Final approval can restrict only an ACTIVE vendor.
             */
            if (vendor.getStatus() != VendorStatus.ACTIVE) {
                throw new ValidationException(
                        "Vendor is no longer ACTIVE. Current status: "
                                + vendor.getStatus(),
                        "ERR_VENDOR_STATUS_CHANGED"
                );
            }

            /*
             * Mark the request as finally approved.
             */
            restrictionRequest.setStatus(
                    VendorRestrictionRequestStatus.FINAL_APPROVED
            );

            /*
             * Apply final restriction to Vendor.
             */
            if (restrictionRequest.getRestrictionType()
                    == VendorRestrictionType.SUSPENSION) {

                vendor.setStatus(VendorStatus.SUSPENDED);

                vendor.setRestrictionStartDate(
                        restrictionRequest.getRestrictionStartDate()
                );

                vendor.setRestrictionEndDate(
                        restrictionRequest.getRestrictionEndDate()
                );

            } else if (restrictionRequest.getRestrictionType()
                    == VendorRestrictionType.BLACKLIST) {

                vendor.setStatus(VendorStatus.BLACKLISTED);

                /*
                 * Blacklist has no suspension date range.
                 */
                vendor.setRestrictionStartDate(null);
                vendor.setRestrictionEndDate(null);
            }

            vendor.setRestrictionReason(
                    restrictionRequest.getReason()
            );

            vendor.setRestrictedBy(userId);
            vendor.setRestrictedAt(reviewTime);

            /*
             * Existing Vendor audit fields.
             */
            vendor.setUpdatedBy(userId);
            vendor.setUpdatedDate(new Date());

            vendorRepository.save(vendor);

        } else {

            /*
             * Admin rejected the request.
             * Vendor remains ACTIVE.
             */
            restrictionRequest.setStatus(
                    VendorRestrictionRequestStatus.ADMIN_REJECTED
            );
        }

        VendorRestrictionRequest savedRequest =
                vendorRestrictionRequestRepository.save(
                        restrictionRequest
                );

        String requestedByName =
                findUserName(savedRequest.getRequestedBy());

        String accountsReviewedByName =
                findUserName(savedRequest.getAccountsReviewedBy());

        return VendorRestrictionResponseDto.builder()
                .id(savedRequest.getId())
                .vendorId(vendor.getId())
                .vendorName(vendor.getName())
                .restrictionType(
                        savedRequest.getRestrictionType()
                )
                .status(savedRequest.getStatus())
                .reason(savedRequest.getReason())
                .restrictionStartDate(
                        savedRequest.getRestrictionStartDate()
                )
                .restrictionEndDate(
                        savedRequest.getRestrictionEndDate()
                )
                .attachmentUrl(
                        savedRequest.getAttachmentUrl()
                )
                .requestedBy(
                        savedRequest.getRequestedBy()
                )
                .requestedByName(requestedByName)
                .requestedAt(
                        savedRequest.getRequestedAt()
                )
                .accountsReviewedBy(
                        savedRequest.getAccountsReviewedBy()
                )
                .accountsReviewedByName(
                        accountsReviewedByName
                )
                .accountsReviewedAt(
                        savedRequest.getAccountsReviewedAt()
                )
                .accountsRemarks(
                        savedRequest.getAccountsRemarks()
                )
                .adminReviewedBy(
                        savedRequest.getAdminReviewedBy()
                )
                .adminReviewedByName(
                        adminUser.getFullName()
                )
                .adminReviewedAt(
                        savedRequest.getAdminReviewedAt()
                )
                .adminRemarks(
                        savedRequest.getAdminRemarks()
                )
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendorRestrictionResponseDto> getAccountsRestrictionRequests(
            Long userId,
            int page,
            int size,
            VendorRestrictionRequestStatus status) {

        if (userId == null) {
            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_ACTIVE_USER_NOT_FOUND"
                ));

        /*
         * Add your Accounts department validation here.
         */

        VendorRestrictionRequestStatus effectiveStatus =
                status != null
                        ? status
                        : VendorRestrictionRequestStatus.PENDING_ACCOUNTS;

        List<VendorRestrictionRequestStatus> accountsAllowedStatuses =
                List.of(
                        VendorRestrictionRequestStatus.PENDING_ACCOUNTS,
                        VendorRestrictionRequestStatus.ACCOUNTS_REJECTED,
                        VendorRestrictionRequestStatus.PENDING_ADMIN,
                        VendorRestrictionRequestStatus.ADMIN_REJECTED,
                        VendorRestrictionRequestStatus.FINAL_APPROVED
                );

        if (!accountsAllowedStatuses.contains(effectiveStatus)) {
            throw new ValidationException(
                    "Invalid restriction request status for Accounts: "
                            + effectiveStatus,
                    "ERR_INVALID_ACCOUNTS_RESTRICTION_STATUS"
            );
        }

        Pageable pageable =
                createRestrictionPageable(page, size);

        Page<VendorRestrictionRequest> requests =
                vendorRestrictionRequestRepository.findRequestsByStatus(
                        effectiveStatus,
                        pageable
                );

        return requests.map(this::mapRestrictionToResponse);
    }

    private Pageable createRestrictionPageable(int page, int size) {

        int pageIndex = page <= 0 ? 0 : page - 1;
        int pageSize = size <= 0 ? 10 : Math.min(size, 100);

        return PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "requestedAt"
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendorRestrictionResponseDto> getAdminRestrictionRequests(
            Long userId,
            int page,
            int size,
            VendorRestrictionRequestStatus status) {

        if (userId == null) {
            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_ACTIVE_USER_NOT_FOUND"
                ));

        /*
         * Add your Admin department validation here.
         */

        VendorRestrictionRequestStatus effectiveStatus =
                status != null
                        ? status
                        : VendorRestrictionRequestStatus.PENDING_ADMIN;

        List<VendorRestrictionRequestStatus> adminAllowedStatuses =
                List.of(
                        VendorRestrictionRequestStatus.PENDING_ADMIN,
                        VendorRestrictionRequestStatus.ADMIN_REJECTED,
                        VendorRestrictionRequestStatus.FINAL_APPROVED
                );

        if (!adminAllowedStatuses.contains(effectiveStatus)) {
            throw new ValidationException(
                    "Invalid restriction request status for Admin: "
                            + effectiveStatus,
                    "ERR_INVALID_ADMIN_RESTRICTION_STATUS"
            );
        }

        Pageable pageable =
                createRestrictionPageable(page, size);

        Page<VendorRestrictionRequest> requests =
                vendorRestrictionRequestRepository.findRequestsByStatus(
                        effectiveStatus,
                        pageable
                );

        return requests.map(this::mapRestrictionToResponse);
    }

    private String findUserName(Long userId) {

        if (userId == null) {
            return null;
        }

        return userRepository
                .findById(userId)
                .map(User::getFullName)
                .orElse(null);
    }

    private String normalizeNullableText(String value) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }


    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }


    @Override
    @Transactional
    public VendorResponseDto updateVendor(Long id, Long userId, VendorRequestDto dto) {

        Vendor vendor = vendorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor not found",
                        "ERR_VENDOR_NOT_FOUND"
                ));

        User updatedByUser = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "UpdatedBy user not found",
                        "ERR_USER_NOT_FOUND"
                ));

        mapDtoToEntity(dto, vendor);

        vendor.setUpdatedBy(updatedByUser.getId());
        vendor.setUpdatedDate(new Date());

        vendor = vendorRepository.save(vendor);

        logger.info("Vendor updated successfully with ID: {}", vendor.getId());

        return mapEntityToDto(vendor);
    }


    @Override
    public VendorResponseDto getVendorById(Long id) {
        Vendor vendor = vendorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found", "ERR_VENDOR_NOT_FOUND"));
        return mapEntityToDto(vendor);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorResponseDto getVendorDetailsById(Long id) {

        Vendor vendor = vendorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor not found",
                        "ERR_VENDOR_NOT_FOUND"
                ));

        VendorResponseDto response = mapEntityToDto(vendor);

        response.setRfqs(
                rfqVendorRepository
                        .findByVendor_IdAndIsDeletedFalseOrderByCreatedDateDesc(id)
                        .stream()
                        .map(rfqVendor -> {
                            RFQVendorResponseDto dto = new RFQVendorResponseDto();
                            dto.setRfqVendorId(rfqVendor.getId());

                            if (rfqVendor.getVendor() != null) {
                                dto.setVendorId(rfqVendor.getVendor().getId());
                                dto.setVendorName(rfqVendor.getVendor().getName());
                                dto.setVendorEmail(rfqVendor.getVendor().getEmail());
                                dto.setVendorMobile(rfqVendor.getVendor().getMobile());
                                dto.setGstNumber(rfqVendor.getVendor().getGstNumber());
                                dto.setPanNumber(rfqVendor.getVendor().getPanNumber());
                                dto.setVendorStatus(
                                        rfqVendor.getVendor().getStatus() != null
                                                ? rfqVendor.getVendor().getStatus().name()
                                                : null
                                );
                            }

                            return dto;
                        })
                        .toList()
        );

        response.setQuotations(
                vendorQuotationRepository
                        .getQuotationsByVendorId(id)
                        .stream()
                        .map(this::mapQuotationToResponse)
                        .toList()
        );

        response.setFinalizations(
                vendorFinalizationRepository
                        .findByVendor_IdAndIsDeletedFalseOrderByCreatedDateDesc(id)
                        .stream()
                        .map(this::mapFinalizationToResponse)
                        .toList()
        );

        response.setOnboardingForms(
                vendorOnboardingRepository
                        .findByVendorFinalization_Vendor_IdAndIsDeletedFalseOrderByCreatedDateDesc(id)
                        .stream()
                        .map(this::mapOnboardingToResponse)
                        .toList()
        );

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendorResponseDto> getAllVendors(
            Long userId,
            int page,
            int size,
            String keyword,
            VendorStatus status
    ) {

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        int pageIndex = page <= 0 ? 0 : page - 1;
        int pageSize = size <= 0 ? 10 : size;

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by("createdDate").descending()
        );

        String normalizedKeyword =
                keyword != null && !keyword.trim().isEmpty()
                        ? keyword.trim()
                        : null;

        Page<Vendor> vendors =
                vendorRepository.searchVendors(
                        normalizedKeyword,
                        status,
                        pageable
                );

        return vendors.map(this::mapEntityToDto);
    }

    @Override
    public void deleteVendor(Long id) {
        Vendor vendor = vendorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found", "ERR_VENDOR_NOT_FOUND"));

        vendor.setDeleted(true);
        vendor.setUpdatedDate(new Date());
        vendorRepository.save(vendor);
        logger.info("Vendor soft deleted with ID: {}", id);
    }

    // ==================== Mapping Methods ====================

    private void mapDtoToEntity(VendorRequestDto dto, Vendor vendor) {

        if (dto.getName() != null) {
            vendor.setName(dto.getName().trim());
        }

        vendor.setDescription(dto.getDescription());
        vendor.setEmail(normalize(dto.getEmail()));
        vendor.setMobile(normalize(dto.getMobile()));
        vendor.setGstNumber(normalize(dto.getGstNumber()));
        vendor.setPanNumber(normalize(dto.getPanNumber()));

    }
    private VendorResponseDto mapEntityToDto(Vendor vendor) {
        VendorResponseDto dto = new VendorResponseDto();

        dto.setId(vendor.getId());
        dto.setName(vendor.getName());
        dto.setDescription(vendor.getDescription());
        dto.setEmail(vendor.getEmail());
        dto.setMobile(vendor.getMobile());
        dto.setGstNumber(vendor.getGstNumber());
        dto.setGstRegistrationType(vendor.getGstRegistrationType());
        dto.setPanNumber(vendor.getPanNumber());
        dto.setStatus(vendor.getStatus());

        dto.setCreatedBy(vendor.getCreatedBy());
        dto.setUpdatedBy(vendor.getUpdatedBy());
        dto.setCreatedDate(vendor.getCreatedDate());
        dto.setUpdatedDate(vendor.getUpdatedDate());
        dto.setDeleted(vendor.isDeleted());

        return dto;
    }

    private VendorQuotationResponseDto mapQuotationToResponse(VendorQuotation quotation) {
        VendorQuotationResponseDto dto = new VendorQuotationResponseDto();

        dto.setId(quotation.getId());

        if (quotation.getRfq() != null) {
            dto.setRfqId(quotation.getRfq().getId());
        }

        if (quotation.getRfqVendor() != null) {
            dto.setRfqVendorId(quotation.getRfqVendor().getId());
        }

        if (quotation.getVendor() != null) {
            dto.setVendorId(quotation.getVendor().getId());
            dto.setVendorName(quotation.getVendor().getName());
            dto.setVendorEmail(quotation.getVendor().getEmail());
            dto.setVendorMobile(quotation.getVendor().getMobile());
        }

        dto.setQuotationNumber(quotation.getQuotationNumber());
        dto.setQuotationDate(quotation.getQuotationDate());
        dto.setValidFrom(quotation.getValidFrom());
        dto.setValidTill(quotation.getValidTill());
        dto.setLatest(quotation.isLatest());
        dto.setCurrency(quotation.getCurrency());
        dto.setSubtotalAmount(quotation.getSubtotalAmount());
        dto.setTaxAmount(quotation.getTaxAmount());
        dto.setGrandTotal(quotation.getGrandTotal());
        dto.setDeliveryDays(quotation.getDeliveryDays());
        dto.setPaymentTerms(quotation.getPaymentTerms());
        dto.setWarrantyTerms(quotation.getWarrantyTerms());
        dto.setRemarks(quotation.getRemarks());
        dto.setStatus(quotation.getStatus() != null ? quotation.getStatus().name() : null);
        dto.setCreatedBy(quotation.getCreatedBy());
        dto.setUpdatedBy(quotation.getUpdatedBy());
        dto.setCreatedDate(quotation.getCreatedDate());
        dto.setUpdatedDate(quotation.getUpdatedDate());
        dto.setDeleted(quotation.isDeleted());
        dto.setAgreementFileUrl(quotation.getAgreementFileUrl());

        return dto;
    }

    private VendorFinalizationResponseDto mapFinalizationToResponse(VendorFinalization finalization) {
        VendorFinalizationResponseDto dto = new VendorFinalizationResponseDto();

        dto.setId(finalization.getId());

        if (finalization.getRfq() != null) {
            dto.setRfqId(finalization.getRfq().getId());
            dto.setRfqNumber(finalization.getRfq().getRfqNumber());
        }

        if (finalization.getRfqVendor() != null) {
            dto.setRfqVendorId(finalization.getRfqVendor().getId());
        }

        if (finalization.getVendor() != null) {
            dto.setVendorId(finalization.getVendor().getId());
            dto.setVendorName(finalization.getVendor().getName());
            dto.setVendorEmail(finalization.getVendor().getEmail());
            dto.setVendorMobile(finalization.getVendor().getMobile());
        }

        if (finalization.getQuotation() != null) {
            dto.setQuotationId(finalization.getQuotation().getId());
            dto.setQuotationNumber(finalization.getQuotation().getQuotationNumber());
        }

        if (finalization.getQuotationItem() != null) {
            dto.setQuotationItemId(finalization.getQuotationItem().getId());
            dto.setQuotationItemName(finalization.getQuotationItem().getItemName());
        }

        dto.setDescription(finalization.getDescription());
        dto.setFinalizedQuantity(finalization.getFinalizedQuantity());
        dto.setUnit(finalization.getUnit());
        dto.setFinalizedUnitRate(finalization.getFinalizedUnitRate());
        dto.setFinalizedAmount(finalization.getFinalizedAmount());
        dto.setTaxPercent(finalization.getTaxPercent());
        dto.setTaxAmount(finalization.getTaxAmount());
        dto.setTotalFinalizedAmount(finalization.getTotalFinalizedAmount());
        dto.setFinalizationReason(finalization.getFinalizationReason());
        dto.setRemarks(finalization.getRemarks());
        dto.setStatus(finalization.getStatus() != null ? finalization.getStatus().name() : null);
        dto.setFinalizedBy(finalization.getFinalizedBy());
        dto.setFinalizedDate(finalization.getFinalizedDate());
        dto.setCreatedBy(finalization.getCreatedBy());
        dto.setUpdatedBy(finalization.getUpdatedBy());
        dto.setCreatedDate(finalization.getCreatedDate());
        dto.setUpdatedDate(finalization.getUpdatedDate());
        dto.setDeleted(finalization.isDeleted());

        dto.setSentToAccounts(finalization.isSentToAccounts());
        dto.setSentToAccountsBy(finalization.getSentToAccountsBy());
        dto.setSentToAccountsDate(finalization.getSentToAccountsDate());

        return dto;
    }

    private VendorOnboardingResponseDto mapOnboardingToResponse(VendorOnboarding onboarding) {
        VendorOnboardingResponseDto dto = new VendorOnboardingResponseDto();

        dto.setId(onboarding.getId());
        dto.setOnboardingNumber(onboarding.getOnboardingNumber());
        dto.setServiceCategory(onboarding.getServiceCategory());
        dto.setOnboardedFor(onboarding.getOnboardedFor());
        dto.setRemarks(onboarding.getRemarks());
        dto.setStatus(onboarding.getStatus() != null ? onboarding.getStatus().name() : null);
        dto.setCreatedBy(onboarding.getCreatedBy());
        dto.setUpdatedBy(onboarding.getUpdatedBy());
        dto.setCreatedDate(onboarding.getCreatedDate());
        dto.setUpdatedDate(onboarding.getUpdatedDate());
        dto.setDeleted(onboarding.isDeleted());

        if (onboarding.getVendorFinalization() != null) {
            dto.setVendorFinalizationId(onboarding.getVendorFinalization().getId());

            if (onboarding.getVendorFinalization().getVendor() != null) {
                dto.setVendorId(onboarding.getVendorFinalization().getVendor().getId());
                dto.setVendorName(onboarding.getVendorFinalization().getVendor().getName());
            }
        }

        return dto;
    }
    private VendorRestrictionResponseDto mapRestrictionToResponse(
            VendorRestrictionRequest request) {

        Vendor vendor = request.getVendor();

        return VendorRestrictionResponseDto.builder()
                .id(request.getId())

                .vendorId(
                        vendor != null
                                ? vendor.getId()
                                : null
                )

                .vendorName(
                        vendor != null
                                ? vendor.getName()
                                : null
                )

                .restrictionType(
                        request.getRestrictionType()
                )

                .status(
                        request.getStatus()
                )

                .reason(
                        request.getReason()
                )

                .restrictionStartDate(
                        request.getRestrictionStartDate()
                )

                .restrictionEndDate(
                        request.getRestrictionEndDate()
                )

                .attachmentUrl(
                        request.getAttachmentUrl()
                )

                .requestedBy(
                        request.getRequestedBy()
                )

                .requestedByName(
                        findUserName(request.getRequestedBy())
                )

                .requestedAt(
                        request.getRequestedAt()
                )

                .accountsReviewedBy(
                        request.getAccountsReviewedBy()
                )

                .accountsReviewedByName(
                        findUserName(request.getAccountsReviewedBy())
                )

                .accountsReviewedAt(
                        request.getAccountsReviewedAt()
                )

                .accountsRemarks(
                        request.getAccountsRemarks()
                )

                .adminReviewedBy(
                        request.getAdminReviewedBy()
                )

                .adminReviewedByName(
                        findUserName(request.getAdminReviewedBy())
                )

                .adminReviewedAt(
                        request.getAdminReviewedAt()
                )

                .adminRemarks(
                        request.getAdminRemarks()
                )

                .build();
    }
}