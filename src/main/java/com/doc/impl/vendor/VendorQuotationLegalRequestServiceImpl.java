package com.doc.impl.vendor;

import com.doc.dto.vendor.*;
import com.doc.entity.vendor.*;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.vendor.VendorFinalizationRepository;
import com.doc.repository.vendor.VendorOnboardingDocumentRepository;
import com.doc.repository.vendor.VendorOnboardingRepository;
import com.doc.repository.vendor.VendorQuotationLegalRequestRepository;
import com.doc.repository.vendor.VendorQuotationRepository;
import com.doc.service.vendor.VendorQuotationLegalRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorQuotationLegalRequestServiceImpl
        implements VendorQuotationLegalRequestService {

    private final VendorQuotationRepository vendorQuotationRepository;
    private final VendorQuotationLegalRequestRepository legalRequestRepository;

    // NEW: needed so Legal sees the NDA + Vendor Registration Form that
    // Procurement uploaded during onboarding.
    private final VendorFinalizationRepository vendorFinalizationRepository;
    private final VendorOnboardingRepository vendorOnboardingRepository;
    private final VendorOnboardingDocumentRepository vendorOnboardingDocumentRepository;


    @Override
    @Transactional
    public VendorQuotationLegalResponseDto createLegalRequest(
            VendorQuotationLegalRequestDto requestDto
    ) {
        VendorQuotation quotation = vendorQuotationRepository
                .findByIdAndIsDeletedFalse(requestDto.getVendorQuotationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor quotation not found",
                        "ERR_VENDOR_QUOTATION_NOT_FOUND"
                ));

        boolean hasOpenLegalRequest = legalRequestRepository
                .findTopByVendorQuotation_IdAndIsDeletedFalseOrderByCreatedDateDesc(
                        requestDto.getVendorQuotationId()
                )
                .filter(existing -> existing.getStatus()
                        != VendorQuotationLegalRequestStatus.AGREEMENT_DISAGREED)
                .isPresent();

        if (hasOpenLegalRequest) {
            throw new ValidationException(
                    "An active legal request already exists for this quotation",
                    "ERR_LEGAL_REQUEST_ALREADY_EXISTS"
            );
        }

        VendorQuotationLegalRequest legalRequest = new VendorQuotationLegalRequest();

        legalRequest.setVendorQuotation(quotation);
        legalRequest.setLegalRequestTitle(requestDto.getLegalRequestTitle());
        legalRequest.setNotes(requestDto.getNotes());
        legalRequest.setStatusReason(requestDto.getStatusReason());
        legalRequest.setStatus(VendorQuotationLegalRequestStatus.SERVICE_AGREEMENT_REQUESTED);
        legalRequest.setAssignedToLegal(requestDto.getAssignedToLegal());
        legalRequest.setCreatedBy(requestDto.getCreatedBy());
        legalRequest.setUpdatedBy(requestDto.getCreatedBy());
        legalRequest.setDeleted(false);

        quotation.setStatus(VendorQuotationStatus.AGREEMENT_REQUESTED);
        vendorQuotationRepository.save(quotation);
        VendorQuotationLegalRequest saved = legalRequestRepository.save(legalRequest);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorQuotationLegalResponseDto> getAllLegalRequests(Long assignedToLegal) {

        List<VendorQuotationLegalRequest> legalRequests = (assignedToLegal != null)
                ? legalRequestRepository
                .findByAssignedToLegalAndIsDeletedFalseOrderByCreatedDateDesc(assignedToLegal)
                : legalRequestRepository
                .findByIsDeletedFalseOrderByCreatedDateDesc();

        List<VendorQuotationLegalResponseDto> responseList = new ArrayList<>();

        for (VendorQuotationLegalRequest legalRequest : legalRequests) {
            responseList.add(mapToResponse(legalRequest));
        }

        return responseList;
    }


    @Override
    @Transactional
    public VendorQuotationLegalResponseDto sendAgreementToProcurement(
            Long id,
            Long userId,
            SendAgreementToProcurementRequestDto requestDto
    ) {
        VendorQuotationLegalRequest legalRequest = legalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor quotation legal request not found",
                        "ERR_VENDOR_QUOTATION_LEGAL_REQUEST_NOT_FOUND"
                ));

        if (legalRequest.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Vendor quotation legal request not found",
                    "ERR_VENDOR_QUOTATION_LEGAL_REQUEST_NOT_FOUND"
            );
        }

        if (legalRequest.getStatus() == VendorQuotationLegalRequestStatus.AGREEMENT_AGREED
                || legalRequest.getStatus() == VendorQuotationLegalRequestStatus.AGREEMENT_DISAGREED) {
            throw new ValidationException(
                    "A decision has already been made on this legal request; "
                            + "it cannot be re-sent to procurement",
                    "ERR_LEGAL_REQUEST_ALREADY_DECIDED"
            );
        }

        legalRequest.setAgreementFileUrl(requestDto.getAgreementFileUrl());
        legalRequest.setStatusReason(requestDto.getRemarks());
        legalRequest.setSentToProcurementBy(userId);
        legalRequest.setSentToProcurementDate(new Date());
        legalRequest.setUpdatedBy(userId);
        legalRequest.setStatus(VendorQuotationLegalRequestStatus.AGREEMENT_SENT_TO_PROCUREMENT);
        legalRequest.setExpiryDate(requestDto.getExpiryDate());
        legalRequest.setValidityDays(requestDto.getValidityDays());

        VendorQuotation quotation = legalRequest.getVendorQuotation();

        if (quotation != null) {
            quotation.setAgreementFileUrl(requestDto.getAgreementFileUrl());
            quotation.setStatus(VendorQuotationStatus.AGREEMENT_SENT_TO_PROCUREMENT);
            quotation.setUpdatedBy(userId);
            vendorQuotationRepository.save(quotation);
        }

        return mapToResponse(legalRequestRepository.save(legalRequest));
    }

    @Override
    @Transactional
    public VendorQuotationLegalResponseDto agreementDecision(
            Long id,
            VendorAgreementDecisionRequestDto requestDto
    ) {
        VendorQuotationLegalRequest legalRequest = legalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor quotation legal request not found",
                        "ERR_VENDOR_QUOTATION_LEGAL_REQUEST_NOT_FOUND"
                ));

        if (legalRequest.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Vendor quotation legal request not found",
                    "ERR_VENDOR_QUOTATION_LEGAL_REQUEST_NOT_FOUND"
            );
        }

        if (legalRequest.getStatus() != VendorQuotationLegalRequestStatus.AGREEMENT_SENT_TO_PROCUREMENT) {
            throw new ValidationException(
                    "Agreement decision can be taken only after agreement is sent to procurement",
                    "ERR_AGREEMENT_NOT_SENT_TO_PROCUREMENT"
            );
        }

        if ("AGREED".equalsIgnoreCase(requestDto.getDecision())) {
            legalRequest.setStatus(VendorQuotationLegalRequestStatus.AGREEMENT_AGREED);
        } else if ("DISAGREED".equalsIgnoreCase(requestDto.getDecision())) {
            legalRequest.setStatus(VendorQuotationLegalRequestStatus.AGREEMENT_DISAGREED);
        } else {
            throw new ValidationException(
                    "Decision must be AGREED or DISAGREED",
                    "ERR_INVALID_AGREEMENT_DECISION"
            );
        }

        legalRequest.setDecisionBy(requestDto.getDecisionBy());
        legalRequest.setDecisionDate(new Date());
        legalRequest.setDecisionRemarks(requestDto.getRemarks());
        legalRequest.setUpdatedBy(requestDto.getDecisionBy());

        return mapToResponse(legalRequestRepository.save(legalRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LegalUserWorkloadResponseDto> getAssignedRequestCounts(
            LegalUserWorkloadRequestDto requestDto
    ) {
        if (requestDto == null || requestDto.getUsers() == null) {
            return List.of();
        }

        Map<Long, String> nameById = new LinkedHashMap<>();

        for (LegalUserWorkloadRequestDto.LegalUserDto user : requestDto.getUsers()) {
            if (user != null && user.getId() != null) {
                nameById.putIfAbsent(user.getId(), user.getName());
            }
        }

        if (nameById.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> countByUserId = legalRequestRepository
                .countByAssignedToLegalGrouped(
                        VendorQuotationLegalRequestStatus.SERVICE_AGREEMENT_REQUESTED,
                        nameById.keySet()
                )
                .stream()
                .collect(Collectors.toMap(
                        VendorQuotationLegalRequestRepository
                                .LegalRequestCountProjection::getUserId,
                        VendorQuotationLegalRequestRepository
                                .LegalRequestCountProjection::getTotal
                ));

        List<LegalUserWorkloadResponseDto> response = new ArrayList<>();

        for (Map.Entry<Long, String> entry : nameById.entrySet()) {
            LegalUserWorkloadResponseDto dto = new LegalUserWorkloadResponseDto();

            dto.setUserId(entry.getKey());
            dto.setName(entry.getValue());
            dto.setPendingRequestCount(countByUserId.getOrDefault(entry.getKey(), 0L));

            response.add(dto);
        }

        return response;
    }

    private VendorQuotationLegalResponseDto mapToResponse(
            VendorQuotationLegalRequest legalRequest
    ) {
        VendorQuotationLegalResponseDto response = new VendorQuotationLegalResponseDto();

        response.setId(legalRequest.getId());

        VendorQuotation quotation = legalRequest.getVendorQuotation();

        if (quotation != null) {
            response.setVendorQuotationId(quotation.getId());
            response.setQuotationNumber(quotation.getQuotationNumber());

            if (quotation.getVendor() != null) {
                response.setVendorId(quotation.getVendor().getId());
                response.setVendorName(quotation.getVendor().getName());
            }

            // Quotation PDFs stored in vendor_quotation_documents
            if (quotation.getDocuments() != null) {
                List<VendorQuotationDocumentResponseDto> documentDtos =
                        quotation.getDocuments()
                                .stream()
                                .filter(document -> !document.isDeleted())
                                .map(document -> {
                                    VendorQuotationDocumentResponseDto dto =
                                            new VendorQuotationDocumentResponseDto();

                                    dto.setId(document.getId());
                                    dto.setQuotationId(quotation.getId());
                                    dto.setFileName(document.getFileName());
                                    dto.setFileUrl(document.getFileUrl());
                                    dto.setFileType(document.getFileType());
                                    dto.setFileSizeKb(document.getFileSizeKb());
                                    dto.setCreatedBy(document.getCreatedBy());   // FIX: was missing
                                    dto.setCreatedDate(document.getCreatedDate());
                                    dto.setDeleted(document.isDeleted());

                                    return dto;
                                })
                                .toList();

                response.setDocuments(documentDtos);
            }

            // NDA, Vendor Registration Form, etc. stored in
            // vendor_onboarding_documents - a different table from the
            // quotation PDF. Legal needs these to review the vendor.
            response.setOnboardingDocuments(
                    loadOnboardingDocuments(quotation.getId())
            );
        }

        response.setLegalRequestTitle(legalRequest.getLegalRequestTitle());
        response.setNotes(legalRequest.getNotes());
        response.setStatusReason(legalRequest.getStatusReason());

        response.setStatus(
                legalRequest.getStatus() != null
                        ? legalRequest.getStatus().name()
                        : null
        );

        response.setAgreementFileUrl(legalRequest.getAgreementFileUrl());

        response.setAssignedToLegal(legalRequest.getAssignedToLegal());
        response.setCreatedBy(legalRequest.getCreatedBy());
        response.setUpdatedBy(legalRequest.getUpdatedBy());
        response.setCreatedDate(legalRequest.getCreatedDate());
        response.setUpdatedDate(legalRequest.getUpdatedDate());
        response.setDeleted(legalRequest.isDeleted());
        response.setExpiryDate(legalRequest.getExpiryDate());
        response.setValidityDays(legalRequest.getValidityDays());

        return response;
    }

    private List<VendorOnboardingDocumentResponseDto> loadOnboardingDocuments(
            Long quotationId
    ) {
        List<VendorFinalization> finalizations =
                vendorFinalizationRepository.findByQuotation_IdAndIsDeletedFalse(quotationId);

        if (finalizations.isEmpty()) {
            return List.of();
        }

        List<Long> onboardingIds = new ArrayList<>();
        Map<Long, String> onboardingNumberById = new HashMap<>();

        for (VendorFinalization finalization : finalizations) {
            vendorOnboardingRepository
                    .findByVendorFinalization_IdAndIsDeletedFalse(finalization.getId())
                    .ifPresent(onboarding -> {
                        onboardingIds.add(onboarding.getId());
                        onboardingNumberById.put(
                                onboarding.getId(),
                                onboarding.getOnboardingNumber()
                        );
                    });
        }

        if (onboardingIds.isEmpty()) {
            return List.of();
        }

        return vendorOnboardingDocumentRepository
                .findByVendorOnboarding_IdInAndIsDeletedFalseOrderByUploadedDateAsc(onboardingIds)
                .stream()
                .map(doc -> {
                    VendorOnboardingDocumentResponseDto dto =
                            new VendorOnboardingDocumentResponseDto();

                    Long onboardingId = doc.getVendorOnboarding() != null
                            ? doc.getVendorOnboarding().getId()
                            : null;

                    dto.setId(doc.getId());
                    dto.setOnboardingId(onboardingId);
                    dto.setOnboardingNumber(
                            onboardingId != null
                                    ? onboardingNumberById.get(onboardingId)
                                    : null
                    );
                    dto.setDocumentType(
                            doc.getDocumentType() != null
                                    ? doc.getDocumentType().name()
                                    : null
                    );
                    dto.setFileName(doc.getFileName());
                    dto.setFileUrl(doc.getFileUrl());
                    dto.setRemarks(doc.getRemarks());
                    dto.setUploadedBy(doc.getUploadedBy());
                    dto.setUploadedDate(doc.getUploadedDate());
                    dto.setDeleted(doc.isDeleted());

                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VendorLegalSummaryResponseDto getSummary(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        List<VendorQuotationLegalRequestRepository.VendorLegalStatusCountProjection> rows =
                legalRequestRepository.countGroupedByStatusForUser(userId);

        List<VendorLegalStatusCountDto> statusCounts = rows.stream()
                .map(r -> new VendorLegalStatusCountDto(r.getStatus().name(), r.getTotal()))
                .toList();

        long totalPending = statusCounts.stream()
                .filter(s -> VendorQuotationLegalRequestStatus.SERVICE_AGREEMENT_REQUESTED
                        .name().equals(s.getStatus()))
                .mapToLong(VendorLegalStatusCountDto::getCount)
                .findFirst()
                .orElse(0L);

        return new VendorLegalSummaryResponseDto(totalPending, statusCounts);
    }
}