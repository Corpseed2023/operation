package com.doc.impl.vendor;

import com.doc.dto.vendor.*;
import com.doc.entity.department.Department;
import com.doc.entity.user.User;
import com.doc.entity.vendor.*;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.ProcurementMilestoneAssignmentRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.projection.*;
import com.doc.repository.vendor.*;
import com.doc.service.vendor.ProductVendorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Service implementation for Product Vendor Dashboard.
 *
 * This service prepares dashboard count/statistics for one product, such as:
 * 1. Total vendors mapped with the product
 * 2. Total finalized vendors
 * 3. Active RFQs
 * 4. Quotations received
 * 5. Lowest finalized vendor and price
 */
@Service
@RequiredArgsConstructor
public class ProductVendorDashboardServiceImpl implements ProductVendorDashboardService {

    private final ProductVendorMappingRepository productVendorMappingRepository;
    private final VendorFinalizationRepository vendorFinalizationRepository;
    private final VendorRFQRepository vendorRFQRepository;
    private final VendorQuotationRepository vendorQuotationRepository;
    private final ProcurementMilestoneAssignmentRepository procurementMilestoneAssignmentRepository;
    private final ProductVendorDashboardRepository productVendorDashboardRepository;
    private final ProcurementPaymentRequestRepository procurementPaymentRequestRepository;
    private final RFQVendorRepository rfqVendorRepository;
    private final UserRepository userRepository;

    /**
     * Fetches all dashboard count data for a given product.
     *
     * @param productId Product ID for which dashboard counts are required
     * @return ProductVendorDashboardCountDto containing dashboard counts and lowest finalized vendor details
     */
    @Override
    @Transactional(readOnly = true)
    public ProductVendorDashboardCountDto getProductVendorDashboardCounts(Long productId) {

        /*
         * Count all active vendors mapped with this product.
         *
         * Example:
         * Product = NBFC Registration
         * Vendors mapped = Raj Traders, Balaji Traders, Legal Firm A
         */
        Long totalVendorCount =
                productVendorMappingRepository.countActiveVendorsByProductId(productId);

        /*
         * These statuses are considered as "final vendor" statuses.
         *
         * FINALIZED              -> Procurement finalized vendor
         * SENT_TO_ACCOUNTS       -> Sent to accounts for approval
         * ACCOUNTS_APPROVED      -> Accounts approved the vendor
         * ONBOARDING_STARTED     -> Vendor onboarding process started
         */
        List<VendorFinalizationStatus> finalVendorStatuses = List.of(
                VendorFinalizationStatus.FINALIZED,
                VendorFinalizationStatus.SENT_TO_ACCOUNTS,
                VendorFinalizationStatus.ACCOUNTS_APPROVED,
                VendorFinalizationStatus.ONBOARDING_STARTED
        );

        /*
         * Count distinct vendors finalized for this product.
         *
         * Distinct count is used because one vendor may have multiple finalization records
         * due to revisions or multiple quotations.
         */
        Long totalFinalVendorCount =
                vendorFinalizationRepository.countDistinctFinalizedVendorsByProductId(
                        productId,
                        finalVendorStatuses
                );

        /*
         * Count active RFQs for this product.
         *
         * CLOSED and CANCELLED RFQs are excluded.
         * Remaining RFQs are treated as active.
         */
        Long activeRfqCount =
                vendorRFQRepository.countActiveRfqsByProductId(
                        productId,
                        List.of(
                                RFQStatus.CLOSED,
                                RFQStatus.CANCELLED
                        )
                );

        /*
         * Count quotations received for this product.
         *
         * These statuses mean quotation has entered the quotation flow
         * and should be counted as received.
         */
        Long quotationReceivedCount =
                vendorQuotationRepository.countReceivedQuotationsByProductId(
                        productId,
                        List.of(
                                VendorQuotationStatus.SUBMITTED,
                                VendorQuotationStatus.REVISED,
                                VendorQuotationStatus.UNDER_COMPARISON,
                                VendorQuotationStatus.ACCEPTED,
                                VendorQuotationStatus.PARTIALLY_ACCEPTED,
                                VendorQuotationStatus.REJECTED,
                                VendorQuotationStatus.AGREEMENT_SENT_TO_PROCUREMENT,
                                VendorQuotationStatus.AGREEMENT_SENT_TO_VENDOR,
                                VendorQuotationStatus.REJECTED_BY_ACCOUNTS
                        )
                );

        /*
         * Fetch the lowest finalized vendor for this product.
         *
         * PageRequest.of(0, 1) means:
         * - first page
         * - only one record
         *
         * Repository query should sort by finalized amount in ascending order.
         */
        List<VendorFinalization> lowestFinalizations =
                vendorFinalizationRepository.findLowestFinalizedVendorByProductId(
                        productId,
                        finalVendorStatuses,
                        PageRequest.of(0, 1)
                );

        /*
         * Default lowest vendor values.
         *
         * These will remain null if no finalized vendor is available.
         */
        Long lowestFinalizedVendorId = null;
        String lowestFinalizedVendorName = null;
        BigDecimal lowestFinalizedPrice = null;

        /*
         * If a lowest finalized vendor exists, extract vendor details and finalized price.
         */
        if (!lowestFinalizations.isEmpty()) {
            VendorFinalization lowest = lowestFinalizations.get(0);

            /*
             * Vendor can be null in case of incomplete or broken data,
             * so null check is added for safety.
             */
            if (lowest.getVendor() != null) {
                lowestFinalizedVendorId = lowest.getVendor().getId();
                lowestFinalizedVendorName = lowest.getVendor().getName();
            }

            /*
             * Total finalized amount is treated as the lowest finalized price.
             */
            lowestFinalizedPrice = lowest.getTotalFinalizedAmount();
        }

        /*
         * Build and return dashboard response DTO.
         */
        return new ProductVendorDashboardCountDto(
                productId,
                totalVendorCount,
                totalFinalVendorCount,
                activeRfqCount,
                quotationReceivedCount,
                lowestFinalizedVendorId,
                lowestFinalizedVendorName,
                lowestFinalizedPrice
        );
    }

    @Override
    public List<VendorAssignmentCountProjection> getVendorWiseAssignmentCounts(Long productId,Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID is required"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_USER_NOT_FOUND"
                ));
        return procurementMilestoneAssignmentRepository
                .getVendorWiseAssignmentCountsByProductId(productId);
    }

    @Override
    public ProductVendorDashboardResponse getDashboardByProductId(Long productId,Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID is required"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_USER_NOT_FOUND"
                ));
        ProductVendorDashboardProjection projection =
                productVendorDashboardRepository
                        .getDashboardByProductId(productId);

        return ProductVendorDashboardResponse.builder()
                .productId(productId)
                .registeredVendorCount(
                        valueOrZero(
                                projection.getRegisteredVendorCount()
                        )
                )
                .activeRfqCount(
                        valueOrZero(
                                projection.getActiveRfqCount()
                        )
                )
                .quotationReceivedCount(
                        valueOrZero(
                                projection.getQuotationReceivedCount()
                        )
                )
                .priceComparisonCount(
                        valueOrZero(
                                projection.getPriceComparisonCount()
                        )
                )
                .vendorSelectedCount(
                        valueOrZero(
                                projection.getVendorSelectedCount()
                        )
                )
                .build();
    }

    @Override
    public VendorPaymentSummaryResponse getVendorPaymentSummary(Long productId, Long vendorId,Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID is required"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_USER_NOT_FOUND"
                ));
        VendorPaymentSummaryProjection projection =
                procurementPaymentRequestRepository
                        .getVendorPaymentSummary(
                                vendorId,
                                productId

                        );

        return VendorPaymentSummaryResponse.builder()
                .productId(productId)
                .vendorId(vendorId)
                .paymentGivenAmount(
                        decimalOrZero(
                                projection.getPaymentGivenAmount()
                        )
                )
                .pendingPaymentAmount(
                        decimalOrZero(
                                projection.getPendingPaymentAmount()
                        )
                )
                .paymentReleasedCount(
                        longOrZero(
                                projection.getPaymentReleasedCount()
                        )
                )
                .pendingPaymentCount(
                        longOrZero(
                                projection.getPendingPaymentCount()
                        )
                )
                .build();
    }

    @Override
    public List<ProductRfqDashboardResponse> getRfqDashboard(Long productId,Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID is required"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_USER_NOT_FOUND"
                ));

        return rfqVendorRepository
                .findRfqDashboardByProductId(productId)
                .stream()
                .map(this::mapRfqDashboardResponse)
                .toList();
    }

    @Override
    public ProductVendorVerificationResponse getVendorVerificationByProductId(
            Long productId, Long userId
    ) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID is required"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_USER_NOT_FOUND"
                ));
        List<Vendor> vendors =
                productVendorMappingRepository.findAllVendorsByProductId(productId);

        int verified = 0;
        int notVerified = 0;

        for (Vendor vendor : vendors) {

            if (vendor.getStatus() == VendorStatus.ACTIVE) {
                verified++;
            } else if (vendor.getStatus() == VendorStatus.PROSPECTIVE
                    || vendor.getStatus() == VendorStatus.ONBOARDING) {
                notVerified++;
            }
        }

        return new ProductVendorVerificationResponse(
                productId,
                verified,
                notVerified
        );
    }

    @Override
    public ProductQuotationResponseRate getQuotationResponseRate(
            Long productId, Long userId
    ) {


        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID is required"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_USER_NOT_FOUND"
                ));


        QuotationResponseRateProjection projection =
                rfqVendorRepository.getQuotationResponseRate(productId);

        long totalInvited = projection != null
                && projection.getTotalInvited() != null
                ? projection.getTotalInvited()
                : 0L;

        long responded = projection != null
                && projection.getResponded() != null
                ? projection.getResponded()
                : 0L;

        long pending = Math.max(totalInvited - responded, 0L);

        BigDecimal responseRate = BigDecimal.ZERO;

        if (totalInvited > 0) {
            responseRate = BigDecimal.valueOf(responded)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            BigDecimal.valueOf(totalInvited),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        return ProductQuotationResponseRate.builder()
                .productId(productId)
                .totalInvited(totalInvited)
                .responded(responded)
                .pending(pending)
                .responseRate(responseRate)
                .build();
    }

    private VendorVerificationResponse mapToResponse(Vendor vendor) {

        return new VendorVerificationResponse(
                vendor.getId(),
                vendor.getName(),
                vendor.getEmail(),
                vendor.getMobile(),
                vendor.getGstNumber(),
                vendor.getPanNumber(),
                vendor.getStatus(),
                vendor.getStatus() == VendorStatus.ACTIVE
        );
    }

    private ProductRfqDashboardResponse mapRfqDashboardResponse(
            ProductRfqDashboardProjection projection
    ) {
        return ProductRfqDashboardResponse.builder()
                .rfqId(projection.getRfqId())
                .rfqNumber(projection.getRfqNumber())
                .title(projection.getTitle())
                .quotationSubmissionDeadline(
                        projection.getQuotationSubmissionDeadline()
                )
                .vendorsInvited(
                        valueOrZero(projection.getVendorsInvited())
                )
                .quotationsReceived(
                        valueOrZero(projection.getQuotationsReceived())
                )
                .status(projection.getStatus())
                .build();
    }

    private Long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private BigDecimal decimalOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Long longOrZero(Long value) {
        return value != null ? value : 0L;
    }
}