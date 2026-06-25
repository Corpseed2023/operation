package com.doc.impl.vendor;

import com.doc.dto.vendor.*;
import com.doc.entity.vendor.*;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.vendor.VendorQuotationLegalRequestRepository;
import com.doc.repository.vendor.VendorQuotationRepository;
import com.doc.service.vendor.VendorQuotationLegalRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.doc.entity.vendor.VendorQuotationLegalRequestStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorQuotationLegalRequestServiceImpl
        implements VendorQuotationLegalRequestService {

    private final VendorQuotationRepository vendorQuotationRepository;
    private final VendorQuotationLegalRequestRepository legalRequestRepository;

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

        VendorQuotationLegalRequest saved = legalRequestRepository.save(legalRequest);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorQuotationLegalResponseDto> getAllLegalRequests(Long assignedToLegal) {

        List<VendorQuotationLegalRequest> legalRequests;

        if (assignedToLegal != null) {
            legalRequests =
                    legalRequestRepository
                            .findByAssignedToLegalAndIsDeletedFalseOrderByCreatedDateDesc(
                                    assignedToLegal
                            );
        } else {
            legalRequests =
                    legalRequestRepository
                            .findByIsDeletedFalseOrderByCreatedDateDesc();
        }

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

        legalRequest.setAgreementFileUrl(requestDto.getAgreementFileUrl());
        legalRequest.setStatusReason(requestDto.getRemarks());
        legalRequest.setSentToProcurementBy(userId);
        legalRequest.setSentToProcurementDate(new Date());
        legalRequest.setUpdatedBy(userId);
        legalRequest.setStatus(
                VendorQuotationLegalRequestStatus.AGREEMENT_SENT_TO_PROCUREMENT
        );

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

    private VendorQuotationLegalResponseDto mapToResponse(
            VendorQuotationLegalRequest legalRequest
    ) {
        VendorQuotationLegalResponseDto response =
                new VendorQuotationLegalResponseDto();

        response.setId(legalRequest.getId());

        if (legalRequest.getVendorQuotation() != null) {
            VendorQuotation quotation = legalRequest.getVendorQuotation();

            response.setVendorQuotationId(quotation.getId());
            response.setQuotationNumber(quotation.getQuotationNumber());

            if (quotation.getVendor() != null) {
                response.setVendorId(quotation.getVendor().getId());
                response.setVendorName(quotation.getVendor().getName());
            }
        }

        response.setLegalRequestTitle(legalRequest.getLegalRequestTitle());
        response.setNotes(legalRequest.getNotes());
        response.setStatusReason(legalRequest.getStatusReason());
        response.setStatus(
                legalRequest.getStatus() != null ? legalRequest.getStatus().name() : null
        );

        response.setAgreementFileUrl(legalRequest.getAgreementFileUrl());

        if (legalRequest.getVendorQuotation() != null) {
            VendorQuotation quotation = legalRequest.getVendorQuotation();

            response.setVendorQuotationId(quotation.getId());
            response.setQuotationNumber(quotation.getQuotationNumber());

        }
        response.setAssignedToLegal(legalRequest.getAssignedToLegal());
        response.setCreatedBy(legalRequest.getCreatedBy());
        response.setUpdatedBy(legalRequest.getUpdatedBy());
        response.setCreatedDate(legalRequest.getCreatedDate());
        response.setUpdatedDate(legalRequest.getUpdatedDate());
        response.setDeleted(legalRequest.isDeleted());

        return response;
    }


}