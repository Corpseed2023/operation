package com.doc.impl.vendor;

import com.doc.dto.vendor.VendorAgreementDecisionRequestDto;
import com.doc.dto.vendor.VendorAgreementPrepareRequestDto;
import com.doc.dto.vendor.VendorQuotationLegalRequestDto;
import com.doc.dto.vendor.VendorQuotationLegalResponseDto;
import com.doc.entity.vendor.VendorQuotation;
import com.doc.entity.vendor.VendorQuotationLegalRequest;
import com.doc.entity.vendor.VendorQuotationLegalRequestStatus;
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

        response.setAssignedToLegal(legalRequest.getAssignedToLegal());
        response.setCreatedBy(legalRequest.getCreatedBy());
        response.setUpdatedBy(legalRequest.getUpdatedBy());
        response.setCreatedDate(legalRequest.getCreatedDate());
        response.setUpdatedDate(legalRequest.getUpdatedDate());
        response.setDeleted(legalRequest.isDeleted());

        return response;
    }
}