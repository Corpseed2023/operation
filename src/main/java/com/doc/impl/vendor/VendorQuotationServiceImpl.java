package com.doc.impl.vendor;

import com.doc.dto.vendor.VendorQuotationItemRequestDto;
import com.doc.dto.vendor.VendorQuotationItemResponseDto;
import com.doc.dto.vendor.VendorQuotationRequestDto;
import com.doc.dto.vendor.VendorQuotationResponseDto;
import com.doc.dto.vendor.request.VendorQuotationDocumentRequestDto;
import com.doc.dto.mail.MailRequestDto;
import com.doc.dto.vendor.*;
import com.doc.entity.vendor.*;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.vendor.*;
import com.doc.service.mail.MailService;
import com.doc.service.vendor.VendorQuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorQuotationServiceImpl implements VendorQuotationService {

    private final VendorQuotationRepository vendorQuotationRepository;
    private final VendorQuotationLegalRequestRepository vendorQuotationLegalRequestRepository;
    private final VendorRFQRepository rfqRepository;
    private final RFQVendorRepository rfqVendorRepository;
    private final VendorRepository vendorRepository;
    private final MailService mailService;

    @Override
    @Transactional
    public VendorQuotationResponseDto createVendorQuotation(VendorQuotationRequestDto requestDto) {

        RFQ rfq = rfqRepository.findById(requestDto.getRfqId())
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        RFQVendor rfqVendor = rfqVendorRepository.findById(requestDto.getRfqVendorId())
                .orElseThrow(() -> new RuntimeException("RFQ vendor not found"));

        Vendor vendor = vendorRepository.findById(requestDto.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if (rfqVendor.getRfq() == null || !rfqVendor.getRfq().getId().equals(rfq.getId())) {
            throw new RuntimeException("RFQ Vendor does not belong to selected RFQ");
        }

        if (rfqVendor.getVendor() == null || !rfqVendor.getVendor().getId().equals(vendor.getId())) {
            throw new RuntimeException("Vendor does not match RFQ Vendor");
        }

        if (requestDto.getValidFrom() != null
                && requestDto.getValidTill() != null
                && requestDto.getValidTill().before(requestDto.getValidFrom())) {

            throw new ValidationException(
                    "Valid Till date cannot be before Valid From date",
                    "ERR_INVALID_DATE_RANGE"
            );
        }

        VendorQuotation quotation = new VendorQuotation();

        quotation.setRfq(rfq);
        quotation.setRfqVendor(rfqVendor);
        quotation.setVendor(vendor);

        quotation.setQuotationNumber(requestDto.getQuotationNumber());
        quotation.setQuotationDate(requestDto.getQuotationDate());
        quotation.setValidFrom(requestDto.getValidFrom());
        quotation.setValidTill(requestDto.getValidTill());
        quotation.setCurrency(requestDto.getCurrency() != null ? requestDto.getCurrency() : "INR");
        quotation.setDeliveryDays(requestDto.getDeliveryDays());

        quotation.setPaymentTerms(requestDto.getPaymentTerms());
        quotation.setWarrantyTerms(requestDto.getWarrantyTerms());
        quotation.setRemarks(requestDto.getRemarks());

        quotation.setCreatedBy(requestDto.getCreatedBy());

        /*
         * Multiple quotation documents
         */
        if (requestDto.getDocuments() != null && !requestDto.getDocuments().isEmpty()) {
            for (VendorQuotationDocumentRequestDto documentDto : requestDto.getDocuments()) {

                VendorQuotationDocument document = new VendorQuotationDocument();

                document.setFileName(documentDto.getFileName());
                document.setFileUrl(documentDto.getFileUrl());
                document.setFileType(documentDto.getFileType());
                document.setFileSizeKb(documentDto.getFileSizeKb());
                document.setCreatedBy(requestDto.getCreatedBy());

                quotation.addDocument(document);
            }
        }

        /*
         * Quotation items
         */
        if (requestDto.getItems() != null) {
            for (VendorQuotationItemRequestDto itemDto : requestDto.getItems()) {

                VendorQuotationItem item = new VendorQuotationItem();

                item.setItemType(itemDto.getItemType());
                item.setItemName(itemDto.getItemName());
                item.setDescription(itemDto.getDescription());
                item.setQuantity(itemDto.getQuantity());
                item.setUnit(itemDto.getUnit());
                item.setUnitRate(itemDto.getUnitRate());
                item.setTaxPercent(itemDto.getTaxPercent());
                item.setRemarks(itemDto.getRemarks());
                item.setCreatedBy(requestDto.getCreatedBy());

                quotation.addItem(item);
            }
        }

        VendorQuotation saved = vendorQuotationRepository.save(quotation);

        /*
         * After procurement manually enters quotation,
         * vendor-wise RFQ status should become QUOTATION_RECEIVED.
         */
        rfqVendor.setStatus(RFQVendorStatus.QUOTATION_RECEIVED);
        rfqVendor.setQuotationReceivedDate(new Date());
        rfqVendor.setUpdatedBy(requestDto.getCreatedBy());
        rfqVendorRepository.save(rfqVendor);

        return mapToResponse(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public List<VendorQuotationResponseDto> getVendorQuotationsByRfqId(Long rfqId) {

        List<VendorQuotation> quotations =
                vendorQuotationRepository.findByRfqIdAndIsDeletedFalseOrderByCreatedDateDesc(rfqId);

        List<VendorQuotationResponseDto> responseList = new ArrayList<>();

        for (VendorQuotation quotation : quotations) {
            responseList.add(mapToResponse(quotation));
        }

        return responseList;
    }


    private VendorQuotationResponseDto mapToResponse(VendorQuotation quotation) {

        VendorQuotationResponseDto response = new VendorQuotationResponseDto();

        response.setId(quotation.getId());
        response.setQuotationNumber(quotation.getQuotationNumber());

        response.setRfqId(quotation.getRfq() != null ? quotation.getRfq().getId() : null);
        response.setRfqVendorId(quotation.getRfqVendor() != null ? quotation.getRfqVendor().getId() : null);

        vendorQuotationLegalRequestRepository
                .findTopByVendorQuotation_IdAndIsDeletedFalseOrderByCreatedDateDesc(quotation.getId())
                .ifPresent(legalRequest ->
                        response.setVendorQuotationLegalRequestId(legalRequest.getId())
                );

        if (quotation.getVendor() != null) {
            response.setVendorId(quotation.getVendor().getId());
            response.setVendorName(quotation.getVendor().getName());
            response.setVendorEmail(quotation.getVendor().getEmail());
            response.setVendorMobile(quotation.getVendor().getMobile());
        }

        response.setQuotationDate(quotation.getQuotationDate());
        response.setValidFrom(quotation.getValidFrom());
        response.setValidTill(quotation.getValidTill());
//        response.setVersionNo(quotation.getVersionNo());
        response.setLatest(quotation.isLatest());
        response.setCurrency(quotation.getCurrency());

        response.setSubtotalAmount(quotation.getSubtotalAmount());
        response.setTaxAmount(quotation.getTaxAmount());
        response.setGrandTotal(quotation.getGrandTotal());

        response.setDeliveryDays(quotation.getDeliveryDays());
        response.setPaymentTerms(quotation.getPaymentTerms());
        response.setWarrantyTerms(quotation.getWarrantyTerms());
        response.setRemarks(quotation.getRemarks());
        response.setAgreementFileUrl(quotation.getAgreementFileUrl());

        response.setStatus(quotation.getStatus() != null ? quotation.getStatus().name() : null);

        response.setCreatedBy(quotation.getCreatedBy());
        response.setUpdatedBy(quotation.getUpdatedBy());
        response.setCreatedDate(quotation.getCreatedDate());
        response.setUpdatedDate(quotation.getUpdatedDate());
        response.setDeleted(quotation.isDeleted());

        List<VendorQuotationDocumentResponseDto> documentResponses = new ArrayList<>();

        if (quotation.getDocuments() != null) {
            for (VendorQuotationDocument document : quotation.getDocuments()) {

                if (document.isDeleted()) {
                    continue;
                }

                VendorQuotationDocumentResponseDto documentResponse =
                        new VendorQuotationDocumentResponseDto();

                documentResponse.setId(document.getId());
                documentResponse.setQuotationId(quotation.getId());
                documentResponse.setFileName(document.getFileName());
                documentResponse.setFileUrl(document.getFileUrl());
                documentResponse.setFileType(document.getFileType());
                documentResponse.setFileSizeKb(document.getFileSizeKb());
                documentResponse.setCreatedBy(document.getCreatedBy());
                documentResponse.setCreatedDate(document.getCreatedDate());
                documentResponse.setDeleted(document.isDeleted());

                documentResponses.add(documentResponse);
            }
        }

        response.setDocuments(documentResponses);

        List<VendorQuotationItemResponseDto> itemResponses = new ArrayList<>() ;

        if (quotation.getItems() != null) {
            for (VendorQuotationItem item : quotation.getItems()) {

                VendorQuotationItemResponseDto itemResponse = new VendorQuotationItemResponseDto();

                itemResponse.setId(item.getId());
                itemResponse.setQuotationId(quotation.getId());
                itemResponse.setItemType(item.getItemType() != null ? item.getItemType().name() : null);
                itemResponse.setItemName(item.getItemName());
                itemResponse.setDescription(item.getDescription());

                itemResponse.setQuantity(item.getQuantity());
                itemResponse.setUnit(item.getUnit());
                itemResponse.setUnitRate(item.getUnitRate());

                itemResponse.setAmount(item.getAmount());
                itemResponse.setTaxPercent(item.getTaxPercent());
                itemResponse.setTaxAmount(item.getTaxAmount());
                itemResponse.setTotalAmount(item.getTotalAmount());

                itemResponse.setRemarks(item.getRemarks());
                itemResponse.setCreatedBy(item.getCreatedBy());
                itemResponse.setUpdatedBy(item.getUpdatedBy());
                itemResponse.setCreatedDate(item.getCreatedDate());
                itemResponse.setUpdatedDate(item.getUpdatedDate());
                itemResponse.setDeleted(item.isDeleted());

                itemResponses.add(itemResponse);
            }
        }

        response.setItems(itemResponses);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public VendorQuotationResponseDto getVendorQuotationById(Long id) {

        VendorQuotation quotation = vendorQuotationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Vendor quotation not found"));

        return mapToResponse(quotation);
    }


    @Override
    @Transactional(readOnly = true)
    public List<VendorQuotationResponseDto> getVendorQuotationsByVendorId(Long vendorId) {

        List<VendorQuotation> quotations =
                vendorQuotationRepository.getQuotationsByVendorId(vendorId);

        List<VendorQuotationResponseDto> responseList = new ArrayList<>();

        for (VendorQuotation quotation : quotations) {
            responseList.add(mapToResponse(quotation));
        }

        return responseList;
    }

    @Override
    @Transactional
    public VendorQuotationResponseDto sendAgreementToVendor(
            Long quotationId,
            Long userId,
            SendAgreementToVendorRequestDto requestDto
    ) {
        VendorQuotation quotation = vendorQuotationRepository
                .findByIdAndIsDeletedFalse(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor quotation not found",
                        "ERR_VENDOR_QUOTATION_NOT_FOUND"
                ));

        if (!StringUtils.hasText(requestDto.getAttachmentUrl())) {
            throw new ValidationException(
                    "Agreement attachment URL is required",
                    "ERR_AGREEMENT_ATTACHMENT_REQUIRED"
            );
        }

        Vendor vendor = quotation.getVendor();

        if (vendor == null) {
            throw new ResourceNotFoundException(
                    "Vendor not found for quotation",
                    "ERR_VENDOR_NOT_FOUND"
            );
        }

        String vendorEmail = null;

        if (quotation.getRfqVendor() != null &&
                StringUtils.hasText(quotation.getRfqVendor().getSentToEmail())) {
            vendorEmail = quotation.getRfqVendor().getSentToEmail();
        } else if (StringUtils.hasText(vendor.getEmail())) {
            vendorEmail = vendor.getEmail();
        }

        if (!StringUtils.hasText(vendorEmail)) {
            throw new ValidationException(
                    "Vendor email is missing",
                    "ERR_VENDOR_EMAIL_MISSING"
            );
        }

        sendFinalAgreementMail(
                vendorEmail,
                vendor,
                quotation,
                requestDto
        );

        quotation.setAgreementFileUrl(requestDto.getAttachmentUrl());
        quotation.setRemarks(
                StringUtils.hasText(requestDto.getRemarks())
                        ? requestDto.getRemarks()
                        : quotation.getRemarks()
        );
        quotation.setStatus(VendorQuotationStatus.AGREEMENT_SENT_TO_VENDOR);
        quotation.setUpdatedBy(userId);

        VendorQuotation saved = vendorQuotationRepository.save(quotation);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public VendorQuotationResponseDto updateVendorQuotation(Long id, VendorQuotationRequestDto requestDto) {

        VendorQuotation quotation = vendorQuotationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Vendor quotation not found"));

        RFQ rfq = rfqRepository.findById(requestDto.getRfqId())
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        RFQVendor rfqVendor = rfqVendorRepository.findById(requestDto.getRfqVendorId())
                .orElseThrow(() -> new RuntimeException("RFQ vendor not found"));

        Vendor vendor = vendorRepository.findById(requestDto.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if (rfqVendor.getRfq() == null || !rfqVendor.getRfq().getId().equals(rfq.getId())) {
            throw new RuntimeException("RFQ Vendor does not belong to selected RFQ");
        }

        if (rfqVendor.getVendor() == null || !rfqVendor.getVendor().getId().equals(vendor.getId())) {
            throw new RuntimeException("Vendor does not match RFQ Vendor");
        }

        if (requestDto.getValidFrom() != null
                && requestDto.getValidTill() != null
                && requestDto.getValidTill().before(requestDto.getValidFrom())) {

            throw new ValidationException(
                    "Valid Till date cannot be before Valid From date",
                    "ERR_INVALID_DATE_RANGE"
            );
        }

        quotation.setRfq(rfq);
        quotation.setRfqVendor(rfqVendor);
        quotation.setVendor(vendor);

        quotation.setQuotationNumber(requestDto.getQuotationNumber());
        quotation.setQuotationDate(requestDto.getQuotationDate());
        quotation.setValidFrom(requestDto.getValidFrom());
        quotation.setValidTill(requestDto.getValidTill());

        quotation.setCurrency(requestDto.getCurrency() != null ? requestDto.getCurrency() : "INR");
        quotation.setDeliveryDays(requestDto.getDeliveryDays());

        quotation.setPaymentTerms(requestDto.getPaymentTerms());
        quotation.setWarrantyTerms(requestDto.getWarrantyTerms());
        quotation.setRemarks(requestDto.getRemarks());

        quotation.setUpdatedBy(requestDto.getCreatedBy());

        /*
         * Update quotation documents.
         * Since this is PUT API, old documents are removed and request documents are saved again.
         */
        quotation.getDocuments().clear();

        if (requestDto.getDocuments() != null && !requestDto.getDocuments().isEmpty()) {
            for (VendorQuotationDocumentRequestDto documentDto : requestDto.getDocuments()) {

                VendorQuotationDocument document = new VendorQuotationDocument();

                document.setFileName(documentDto.getFileName());
                document.setFileUrl(documentDto.getFileUrl());
                document.setFileType(documentDto.getFileType());
                document.setFileSizeKb(documentDto.getFileSizeKb());
                document.setCreatedBy(requestDto.getCreatedBy());

                quotation.addDocument(document);
            }
        }

        /*
         * Update quotation items.
         * Old items are removed and new request items are saved again.
         */
        quotation.getItems().clear();

        if (requestDto.getItems() != null) {
            for (VendorQuotationItemRequestDto itemDto : requestDto.getItems()) {

                VendorQuotationItem item = new VendorQuotationItem();

                item.setItemType(itemDto.getItemType());
                item.setItemName(itemDto.getItemName());
                item.setDescription(itemDto.getDescription());
                item.setQuantity(itemDto.getQuantity());
                item.setUnit(itemDto.getUnit());
                item.setUnitRate(itemDto.getUnitRate());
                item.setTaxPercent(itemDto.getTaxPercent());
                item.setRemarks(itemDto.getRemarks());
                item.setCreatedBy(requestDto.getCreatedBy());
                item.setUpdatedBy(requestDto.getCreatedBy());

                quotation.addItem(item);
            }
        }

        VendorQuotation saved = vendorQuotationRepository.save(quotation);

        return mapToResponse(saved);
    }



    private void sendFinalAgreementMail(
            String vendorEmail,
            Vendor vendor,
            VendorQuotation quotation,
            SendAgreementToVendorRequestDto requestDto
    ) {
        String subject = StringUtils.hasText(requestDto.getSubject())
                ? requestDto.getSubject()
                : "Final Service Agreement - " + safe(quotation.getQuotationNumber());

        StringBuilder body = new StringBuilder();

        body.append("<p>Dear ").append(safe(vendor.getName())).append(",</p>");

        if (StringUtils.hasText(requestDto.getMessage())) {
            body.append("<p>").append(requestDto.getMessage()).append("</p>");
        } else {
            body.append("<p>Your final service agreement has been shared by Corpseed Procurement Team.</p>");
        }

        body.append("<p><b>Quotation Number:</b> ")
                .append(safe(quotation.getQuotationNumber()))
                .append("</p>");

        if (quotation.getRfq() != null) {
            body.append("<p><b>RFQ ID:</b> ")
                    .append(quotation.getRfq().getId())
                    .append("</p>");
        }

        body.append("<p><b>Agreement Attachment:</b></p>");
        body.append("<ul>");
        body.append("<li>")
                .append("<a href='")
                .append(requestDto.getAttachmentUrl())
                .append("' target='_blank'>")
                .append("View Final Agreement")
                .append("</a>")
                .append("</li>");
        body.append("</ul>");

        if (StringUtils.hasText(requestDto.getRemarks())) {
            body.append("<p><b>Remarks:</b> ")
                    .append(requestDto.getRemarks())
                    .append("</p>");
        }

        body.append("<p>Regards,<br/>Corpseed Procurement Team</p>");

        MailRequestDto mailRequestDto = MailRequestDto.builder()
                .to(vendorEmail)
                .subject(subject)
                .body(body.toString())
                .html(true)
                .build();

        mailService.sendMail(mailRequestDto);
    }
    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}