package com.doc.impl.legalDashboard;

import com.doc.dto.legalDashbaord.CompanyLegalClientDto;
import com.doc.dto.legalDashbaord.PaymentLegalClientDto;
import com.doc.dto.legalDashbaord.PendingLegalQueueItemDto;
import com.doc.em.LegalStatus;
import com.doc.entity.legalrequest.LegalRequest;
import com.doc.entity.vendor.VendorQuotationLegalRequest;
import com.doc.feign.AccountFeignClient;
import com.doc.feign.LeadFeignClient;
import com.doc.repository.LegalRequestRepository;
import com.doc.repository.vendor.VendorQuotationLegalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalDashboardAggregationService {

    private final LegalRequestRepository legalRequestRepository;
    private final VendorQuotationLegalRequestRepository vendorLegalRequestRepository;
    private final LeadFeignClient leadServiceClient;
    private final AccountFeignClient accountServiceClient;

    public List<PendingLegalQueueItemDto> getPendingLegalQueue(Long userId) {

        List<PendingLegalQueueItemDto> items = new ArrayList<>();

        items.addAll(fetchProjectLegalItems(userId));
        items.addAll(fetchVendorLegalItems(userId));
        items.addAll(fetchCompanyLegalItems(userId));
        items.addAll(fetchPaymentLegalItems(userId));

        items.sort(
                Comparator.comparing(
                        PendingLegalQueueItemDto::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        return items;
    }

    private List<PendingLegalQueueItemDto> fetchProjectLegalItems(Long userId) {

        List<LegalRequest> requests = legalRequestRepository
                .findByUserRelatedAndStatusNative(
                        userId,
                        LegalStatus.INITIATED.name(),
                        PageRequest.of(0, 10)
                )
                .getContent();

        return requests.stream()
                .map(r -> new PendingLegalQueueItemDto(
                        "PROJECT",
                        r.getId(),
                        r.getLegalRequestTitle(),
                        "Status: " + (r.getLegalStatus() != null ? r.getLegalStatus().name() : "-"),
                        r.getCreatedAt()
                ))
                .toList();
    }

    private List<PendingLegalQueueItemDto> fetchVendorLegalItems(Long userId) {

        List<VendorQuotationLegalRequest> requests = vendorLegalRequestRepository
                .findByAssignedToLegalAndIsDeletedFalseOrderByCreatedDateDesc(userId);

        return requests.stream()
                .map(r -> new PendingLegalQueueItemDto(
                        "VENDOR",
                        r.getId(),
                        r.getLegalRequestTitle(),
                        r.getVendorQuotation() != null
                                && r.getVendorQuotation().getVendor() != null
                                ? "Vendor: " + r.getVendorQuotation().getVendor().getName()
                                : "Vendor agreement request",
                        toLocalDateTime(r.getCreatedDate())
                ))
                .toList();
    }

    private List<PendingLegalQueueItemDto> fetchCompanyLegalItems(Long userId) {

        try {
            List<CompanyLegalClientDto> requests =
                    leadServiceClient.getPendingCompanyLegalRequests(userId);

            if (requests == null) {
                return List.of();
            }

            return requests.stream()
                    .map(r -> new PendingLegalQueueItemDto(
                            "COMPANY",
                            r.getId(),
                            r.getCompanyName() != null ? r.getCompanyName() : "Company verification",
                            "Document: " + (r.getDocumentType() != null ? r.getDocumentType() : "-"),
                            r.getCreatedAt()
                    ))
                    .toList();

        } catch (Exception ex) {
            // Lead Service being down/slow should degrade the queue,
            // not break the whole dashboard response.
            log.warn("[LEGAL-QUEUE] Failed to fetch company legal items from Lead Service | userId={} | error={}",
                    userId, ex.getMessage());
            return List.of();
        }
    }

    private List<PendingLegalQueueItemDto> fetchPaymentLegalItems(Long userId) {

        try {
            List<PaymentLegalClientDto> requests =
                    accountServiceClient.getPendingPaymentLegalRequests(userId);

            if (requests == null) {
                return List.of();
            }

            return requests.stream()
                    .map(r -> new PendingLegalQueueItemDto(
                            "PAYMENT",
                            r.getId(),
                            r.getCompanyName() != null
                                    ? r.getCompanyName() + " — PO verification"
                                    : "PO verification",
                            r.getUnbilledNumber() != null
                                    ? "Unbilled: " + r.getUnbilledNumber()
                                    : "Purchase order legal check",
                            r.getCreatedAt()
                    ))
                    .toList();

        } catch (Exception ex) {
            log.warn("[LEGAL-QUEUE] Failed to fetch payment legal items from Account Service | userId={} | error={}",
                    userId, ex.getMessage());
            return List.of();
        }
    }

    private LocalDateTime toLocalDateTime(java.util.Date date) {
        return date == null
                ? null
                : date.toInstant()
                .atZone(java.time.ZoneId.of("Asia/Kolkata"))
                .toLocalDateTime();
    }
}