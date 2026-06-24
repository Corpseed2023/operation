package com.doc.impl.vendor;

import com.doc.dto.mail.MailRequestDto;
import com.doc.dto.vendor.VendorOnboardingDocumentRequestDto;
import com.doc.dto.vendor.VendorOnboardingResponseDto;
import com.doc.dto.vendor.VendorOnboardingSendFormRequestDto;
import com.doc.entity.vendor.*;
import com.doc.repository.vendor.VendorFinalizationRepository;
import com.doc.repository.vendor.VendorOnboardingDocumentRepository;
import com.doc.repository.vendor.VendorOnboardingRepository;
import com.doc.service.mail.MailService;
import com.doc.service.vendor.VendorOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class VendorOnboardingServiceImpl implements VendorOnboardingService {

    private final VendorFinalizationRepository vendorFinalizationRepository;
    private final VendorOnboardingRepository vendorOnboardingRepository;
    private final VendorOnboardingDocumentRepository vendorOnboardingDocumentRepository;
    private final MailService mailService;

    @Override
    @Transactional
    public VendorOnboardingResponseDto sendOnboardingForm(
            Long vendorFinalizationId,
            VendorOnboardingSendFormRequestDto requestDto
    ) {
        VendorFinalization finalization = vendorFinalizationRepository
                .findByIdAndIsDeletedFalse(vendorFinalizationId)
                .orElseThrow(() -> new RuntimeException("Vendor finalization not found"));

        if (finalization.getVendor() == null) {
            throw new RuntimeException("Vendor not found in finalization");
        }

        Vendor vendor = finalization.getVendor();
        RFQ rfq = finalization.getRfq();
        RFQVendor rfqVendor = finalization.getRfqVendor();

        String vendorEmail = resolveVendorEmail(rfqVendor, vendor);

        vendorOnboardingRepository
                .findByVendorFinalization_IdAndIsDeletedFalse(vendorFinalizationId)
                .ifPresent(existing -> {
                    throw new RuntimeException("Vendor onboarding already started for this finalization");
                });

        VendorOnboarding onboarding = new VendorOnboarding();

        onboarding.setOnboardingNumber(generateOnboardingNumber());
        onboarding.setVendor(vendor);
        onboarding.setRfq(rfq);
        onboarding.setVendorFinalization(finalization);

        onboarding.setServiceCategory(requestDto.getServiceCategory());
        onboarding.setOnboardedFor(
                StringUtils.hasText(requestDto.getOnboardedFor())
                        ? requestDto.getOnboardedFor()
                        : finalization.getDescription()
        );

        onboarding.setFormSentDate(new Date());
        onboarding.setStatus(VendorOnboardingStatus.FORM_SENT_TO_VENDOR);
        onboarding.setRemarks(requestDto.getRemarks());

        onboarding.setCreatedBy(requestDto.getCreatedBy());
        onboarding.setUpdatedBy(requestDto.getCreatedBy());
        onboarding.setDeleted(false);

        if (requestDto.getDocuments() == null || requestDto.getDocuments().isEmpty()) {
            throw new RuntimeException("At least one onboarding document is required");
        }

        VendorOnboarding savedOnboarding = vendorOnboardingRepository.save(onboarding);

        for (VendorOnboardingDocumentRequestDto docDto : requestDto.getDocuments()) {
            VendorOnboardingDocument document = new VendorOnboardingDocument();

            document.setVendorOnboarding(savedOnboarding);
            document.setDocumentType(docDto.getDocumentType());
            document.setFileName(docDto.getFileName());
            document.setFileUrl(docDto.getFileUrl());
            document.setRemarks(docDto.getRemarks());
            document.setUploadedBy(requestDto.getCreatedBy());
            document.setDeleted(false);

            vendorOnboardingDocumentRepository.save(document);
        }

        sendVendorOnboardingMail(
                vendorEmail,
                vendor,
                finalization,
                requestDto
        );

        finalization.setStatus(VendorFinalizationStatus.ONBOARDING_STARTED);
        finalization.setUpdatedBy(requestDto.getCreatedBy());

        vendorFinalizationRepository.save(finalization);

        return mapToResponse(savedOnboarding);
    }

    private String resolveVendorEmail(RFQVendor rfqVendor, Vendor vendor) {
        if (rfqVendor != null && StringUtils.hasText(rfqVendor.getSentToEmail())) {
            return rfqVendor.getSentToEmail();
        }

        if (vendor != null && StringUtils.hasText(vendor.getEmail())) {
            return vendor.getEmail();
        }

        throw new RuntimeException("Vendor email is missing");
    }

    private void sendVendorOnboardingMail(
            String vendorEmail,
            Vendor vendor,
            VendorFinalization finalization,
            VendorOnboardingSendFormRequestDto requestDto
    ) {
        String subject = StringUtils.hasText(requestDto.getSubject())
                ? requestDto.getSubject()
                : "Vendor Onboarding Documents Required - " + safe(vendor.getName());

        StringBuilder body = new StringBuilder();

        body.append("<p>Dear ").append(safe(vendor.getName())).append(",</p>");

        if (StringUtils.hasText(requestDto.getMessage())) {
            body.append("<p>").append(requestDto.getMessage()).append("</p>");
        } else {
            body.append("<p>You have been finalized for vendor onboarding.</p>");
        }

        body.append("<p><b>Finalized Work:</b> ")
                .append(safe(finalization.getDescription()))
                .append("</p>");

        body.append("<p>Please review the shared documents and complete the onboarding formalities.</p>");

        body.append("<p><b>Documents:</b></p>");
        body.append("<ul>");

        if (requestDto.getDocuments() != null) {
            for (VendorOnboardingDocumentRequestDto doc : requestDto.getDocuments()) {
                body.append("<li>")
                        .append(doc.getDocumentType() != null ? doc.getDocumentType().name() : "DOCUMENT")
                        .append(" - ")
                        .append("<a href='")
                        .append(doc.getFileUrl())
                        .append("' target='_blank'>")
                        .append(StringUtils.hasText(doc.getFileName()) ? doc.getFileName() : "View Document")
                        .append("</a>")
                        .append("</li>");
            }
        }

        body.append("</ul>");
        body.append("<p>Regards,<br/>Corpseed Procurement Team</p>");

        MailRequestDto mailRequestDto = MailRequestDto.builder()
                .to(vendorEmail)
                .subject(subject)
                .body(body.toString())
                .html(true)
                .build();

        mailService.sendMail(mailRequestDto);
    }

    private String generateOnboardingNumber() {
        int year = Year.now().getValue();
        String prefix = "VOB-" + year + "-";

        return vendorOnboardingRepository
                .findTopByOnboardingNumberStartingWithOrderByOnboardingNumberDesc(prefix)
                .map(existing -> {
                    String lastNumber = existing.getOnboardingNumber()
                            .substring(existing.getOnboardingNumber().lastIndexOf("-") + 1);

                    int nextNumber = Integer.parseInt(lastNumber) + 1;

                    return prefix + String.format("%04d", nextNumber);
                })
                .orElse(prefix + "0001");
    }

    private VendorOnboardingResponseDto mapToResponse(VendorOnboarding onboarding) {
        VendorOnboardingResponseDto response = new VendorOnboardingResponseDto();

        response.setId(onboarding.getId());
        response.setOnboardingNumber(onboarding.getOnboardingNumber());

        if (onboarding.getVendor() != null) {
            response.setVendorId(onboarding.getVendor().getId());
            response.setVendorName(onboarding.getVendor().getName());
            response.setVendorEmail(onboarding.getVendor().getEmail());
        }

        if (onboarding.getRfq() != null) {
            response.setRfqId(onboarding.getRfq().getId());
            response.setRfqNumber(onboarding.getRfq().getRfqNumber());
        }

        if (onboarding.getVendorFinalization() != null) {
            response.setVendorFinalizationId(onboarding.getVendorFinalization().getId());
        }

        response.setServiceCategory(onboarding.getServiceCategory());
        response.setOnboardedFor(onboarding.getOnboardedFor());
        response.setFormSentDate(onboarding.getFormSentDate());

        response.setStatus(
                onboarding.getStatus() != null ? onboarding.getStatus().name() : null
        );

        response.setRemarks(onboarding.getRemarks());
        response.setCreatedBy(onboarding.getCreatedBy());
        response.setUpdatedBy(onboarding.getUpdatedBy());
        response.setCreatedDate(onboarding.getCreatedDate());
        response.setUpdatedDate(onboarding.getUpdatedDate());
        response.setDeleted(onboarding.isDeleted());

        return response;
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "N/A";
    }
}