package com.doc.impl.vendor;

import com.doc.dto.mail.MailRequestDto;
import com.doc.dto.vendor.*;
import com.doc.entity.product.Product;
import com.doc.entity.vendor.*;
import com.doc.repository.ProductRepository;
import com.doc.repository.vendor.VendorRFQRepository;
import com.doc.repository.vendor.VendorRepository;
import com.doc.service.mail.MailService;
import com.doc.service.vendor.VendorRFQService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorRFQServiceImpl implements VendorRFQService {

    private final VendorRFQRepository vendorRFQRepository;
    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final MailService mailService;
    private final TemplateEngine templateEngine;

    @Override
    @Transactional
    public RFQResponseDto createRFQ(Long userId, RFQCreateRequestDto requestDto) {


        Product product = productRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException(
                        "Product not found with ID: " + requestDto.getProductId()
                ));

        Set<Long> uniqueVendorIds = new HashSet<>(requestDto.getVendorIds());

        List<Vendor> vendors = new ArrayList<>();

        for (Long vendorId : uniqueVendorIds) {
            Vendor vendor = vendorRepository.findByIdAndIsDeletedFalse(vendorId)
                    .orElseThrow(() -> new RuntimeException(
                            "Vendor not found or deleted with ID: " + vendorId
                    ));

            vendors.add(vendor);
        }

        RFQ rfq = new RFQ();

        rfq.setRfqNumber(generateRFQNumber());
        rfq.setTitle(requestDto.getTitle());
        rfq.setDescription(requestDto.getDescription());
        rfq.setProduct(product);
        rfq.setScopeOfWork(requestDto.getScopeOfWork());
        rfq.setTermsAndConditions(requestDto.getTermsAndConditions());
        rfq.setDeliveryLocation(requestDto.getDeliveryLocation());
        rfq.setQuotationSubmissionDeadline(requestDto.getQuotationSubmissionDeadline());
        rfq.setExpectedStartDate(requestDto.getExpectedStartDate());
        rfq.setExpectedEndDate(requestDto.getExpectedEndDate());
        rfq.setContactPersonName(requestDto.getContactPersonName());
        rfq.setContactPersonEmail(requestDto.getContactPersonEmail());
        rfq.setContactPersonMobile(requestDto.getContactPersonMobile());
        rfq.setAttachmentUrl(requestDto.getAttachmentUrl());

        rfq.setStatus(RFQStatus.DRAFT);
        rfq.setCreatedBy(userId);
        rfq.setUpdatedBy(userId);
        rfq.setDeleted(false);

        for (Vendor vendor : vendors) {
            RFQVendor rfqVendor = new RFQVendor();

            rfqVendor.setRfq(rfq);
            rfqVendor.setVendor(vendor);

            rfq.getVendors().add(rfqVendor);
        }

        RFQ savedRFQ = vendorRFQRepository.save(rfq);

        return mapToResponseDto(savedRFQ);
    }

    @Override
    @Transactional
    public RFQResponseDto updateRFQ(
            Long rfqId,
            Long userId,
            RFQUpdateRequestDto requestDto
    ) {
        RFQ rfq = vendorRFQRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found with ID: " + rfqId));

        if (rfq.isDeleted()) {
            throw new RuntimeException("RFQ is deleted with ID: " + rfqId);
        }

        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException(
                        "Product not found with ID: " + requestDto.getProductId()
                ));

        rfq.setTitle(requestDto.getTitle());
        rfq.setDescription(requestDto.getDescription());
        rfq.setProduct(product);
        rfq.setScopeOfWork(requestDto.getScopeOfWork());
        rfq.setTermsAndConditions(requestDto.getTermsAndConditions());
        rfq.setDeliveryLocation(requestDto.getDeliveryLocation());
        rfq.setQuotationSubmissionDeadline(requestDto.getQuotationSubmissionDeadline());
        rfq.setExpectedStartDate(requestDto.getExpectedStartDate());
        rfq.setExpectedEndDate(requestDto.getExpectedEndDate());
        rfq.setContactPersonName(requestDto.getContactPersonName());
        rfq.setContactPersonEmail(requestDto.getContactPersonEmail());
        rfq.setContactPersonMobile(requestDto.getContactPersonMobile());
        rfq.setAttachmentUrl(requestDto.getAttachmentUrl());
        rfq.setUpdatedBy(userId);

        Set<Long> requestedVendorIds = new HashSet<>(requestDto.getVendorIds());

        List<RFQVendor> existingMappings = rfq.getVendors();

        existingMappings.removeIf(mapping ->
                mapping.getVendor() == null
                        || !requestedVendorIds.contains(mapping.getVendor().getId())
        );

        Set<Long> existingVendorIds = existingMappings.stream()
                .filter(mapping -> mapping.getVendor() != null)
                .map(mapping -> mapping.getVendor().getId())
                .collect(Collectors.toSet());

        for (Long vendorId : requestedVendorIds) {

            if (existingVendorIds.contains(vendorId)) {
                continue;
            }

            Vendor vendor = vendorRepository.findByIdAndIsDeletedFalse(vendorId)
                    .orElseThrow(() -> new RuntimeException(
                            "Vendor not found or deleted with ID: " + vendorId
                    ));

            RFQVendor rfqVendor = new RFQVendor();
            rfqVendor.setRfq(rfq);
            rfqVendor.setVendor(vendor);

            rfqVendor.setCreatedBy(userId);
            rfqVendor.setUpdatedBy(userId);
            rfqVendor.setDeleted(false);

            existingMappings.add(rfqVendor);
        }

        RFQ savedRFQ = vendorRFQRepository.save(rfq);

        return mapToResponseDto(savedRFQ);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RFQResponseDto> getAllRFQs(
            Long productId,
            RFQStatus status,
            Long userId,
            int page,
            int size
    ) {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be greater than 0");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }

        Pageable pageable = PageRequest.of(page - 1, size);

        return vendorRFQRepository
                .getAllRFQs(productId, status, userId, pageable)
                .map(this::mapToResponseDto);
    }

    private String generateRFQNumber() {

        int year = Year.now().getValue();
        String prefix = "RFQ-" + year + "-";

        return vendorRFQRepository
                .findTopByRfqNumberStartingWithOrderByRfqNumberDesc(prefix)
                .map(rfq -> {
                    String lastNumber = rfq.getRfqNumber()
                            .substring(rfq.getRfqNumber().lastIndexOf("-") + 1);

                    int nextNumber = Integer.parseInt(lastNumber) + 1;

                    return prefix + String.format("%04d", nextNumber);
                })
                .orElse(prefix + "0001");
    }

    @Override
    @Transactional
    public RFQResponseDto sendRFQToVendors(
            Long rfqId,
            Long userId,
            RFQSendMailRequestDto requestDto
    ) {
        RFQ rfq = vendorRFQRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found with ID: " + rfqId));

        if (rfq.isDeleted()) {
            throw new RuntimeException("RFQ is deleted with ID: " + rfqId);
        }

        if (rfq.getVendors() == null || rfq.getVendors().isEmpty()) {
            throw new RuntimeException("No vendors mapped with this RFQ");
        }

        List<RFQVendor> vendorsToSend;

        if (requestDto.getRfqVendorIds() == null || requestDto.getRfqVendorIds().isEmpty()) {
            vendorsToSend = rfq.getVendors()
                    .stream()
                    .filter(rfqVendor -> !rfqVendor.isDeleted())
                    .toList();
        } else {
            Set<Long> requestedRfqVendorIds = new HashSet<>(requestDto.getRfqVendorIds());

            vendorsToSend = rfq.getVendors()
                    .stream()
                    .filter(rfqVendor -> !rfqVendor.isDeleted())
                    .filter(rfqVendor -> requestedRfqVendorIds.contains(rfqVendor.getId()))
                    .toList();
        }

        if (vendorsToSend.isEmpty()) {
            throw new RuntimeException("No valid RFQ vendors found for sending mail");
        }

        int sentCount = 0;

        for (RFQVendor rfqVendor : vendorsToSend) {

            Vendor vendor = rfqVendor.getVendor();

            if (vendor == null) {
                continue;
            }

            if (!StringUtils.hasText(vendor.getEmail())) {
                throw new RuntimeException("Vendor email is missing for vendor ID: " + vendor.getId());
            }

            String subject = buildRFQMailSubject(rfq, requestDto);

            String body = buildRFQMailBody(rfq, rfqVendor, vendor, requestDto);

            MailRequestDto mailRequestDto = MailRequestDto.builder()
                    .to(vendor.getEmail())
                    .cc(requestDto.getCc())
                    .bcc(requestDto.getBcc())
                    .subject(subject)
                    .body(body)
                    .html(true)
                    .build();

            mailService.sendMail(mailRequestDto);

            rfqVendor.setStatus(RFQVendorStatus.SENT);
            rfqVendor.setSentDate(new Date());
            rfqVendor.setSentToEmail(vendor.getEmail());
            rfqVendor.setSentToMobile(vendor.getMobile());
            rfqVendor.setUpdatedBy(userId);
            rfqVendor.setRemarks("RFQ sent to vendor by mail");

            sentCount++;
        }

        if (sentCount == 0) {
            throw new RuntimeException("RFQ mail was not sent to any vendor");
        }

        rfq.setStatus(RFQStatus.SENT);
        rfq.setUpdatedBy(userId);

        RFQ savedRFQ = vendorRFQRepository.save(rfq);

        return mapToResponseDto(savedRFQ);
    }

    private String buildRFQMailSubject(
            RFQ rfq,
            RFQSendMailRequestDto requestDto
    ) {
        if (requestDto != null && StringUtils.hasText(requestDto.getSubject())) {
            return requestDto.getSubject();
        }

        return "Request for Quotation - " + rfq.getRfqNumber() + " - " + rfq.getTitle();
    }

    private String buildRFQMailBody(
            RFQ rfq,
            RFQVendor rfqVendor,
            Vendor vendor,
            RFQSendMailRequestDto requestDto
    ) {
        String productName = rfq.getProduct() != null
                ? rfq.getProduct().getProductName()
                : "N/A";

        String customMessage = "";

        if (requestDto != null && StringUtils.hasText(requestDto.getMessage())) {
            customMessage = requestDto.getMessage();
        }

        Context context = new Context();

        context.setVariable("vendorName", safe(vendor.getName()));
        context.setVariable("rfqVendorId", rfqVendor != null ? rfqVendor.getId() : null);

        context.setVariable("customMessage", customMessage);

        context.setVariable("rfqNumber", safe(rfq.getRfqNumber()));
        context.setVariable("rfqTitle", safe(rfq.getTitle()));
        context.setVariable("productName", safe(productName));
        context.setVariable("description", safe(rfq.getDescription()));
        context.setVariable("scopeOfWork", safe(rfq.getScopeOfWork()));
        context.setVariable("termsAndConditions", safe(rfq.getTermsAndConditions()));
        context.setVariable("deliveryLocation", safe(rfq.getDeliveryLocation()));

        context.setVariable(
                "quotationSubmissionDeadline",
                rfq.getQuotationSubmissionDeadline() != null
                        ? rfq.getQuotationSubmissionDeadline().toString()
                        : "N/A"
        );

        context.setVariable(
                "expectedStartDate",
                rfq.getExpectedStartDate() != null
                        ? rfq.getExpectedStartDate().toString()
                        : "N/A"
        );

        context.setVariable(
                "expectedEndDate",
                rfq.getExpectedEndDate() != null
                        ? rfq.getExpectedEndDate().toString()
                        : "N/A"
        );

        context.setVariable("contactPersonName", safe(rfq.getContactPersonName()));
        context.setVariable("contactPersonEmail", safe(rfq.getContactPersonEmail()));
        context.setVariable("contactPersonMobile", safe(rfq.getContactPersonMobile()));

        context.setVariable("hasAttachment", StringUtils.hasText(rfq.getAttachmentUrl()));
        context.setVariable("attachmentUrl", rfq.getAttachmentUrl());

        return templateEngine.process("mail/rfq-mail", context);
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "N/A";
    }

    private RFQResponseDto mapToResponseDto(RFQ rfq) {

        if (rfq == null) {
            return null;
        }

        Product product = rfq.getProduct();

        return RFQResponseDto.builder()
                .id(rfq.getId())
                .rfqNumber(rfq.getRfqNumber())
                .title(rfq.getTitle())
                .description(rfq.getDescription())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getProductName() : null)
                .scopeOfWork(rfq.getScopeOfWork())
                .termsAndConditions(rfq.getTermsAndConditions())
                .deliveryLocation(rfq.getDeliveryLocation())
                .quotationSubmissionDeadline(rfq.getQuotationSubmissionDeadline())
                .expectedStartDate(rfq.getExpectedStartDate())
                .expectedEndDate(rfq.getExpectedEndDate())
                .contactPersonName(rfq.getContactPersonName())
                .contactPersonEmail(rfq.getContactPersonEmail())
                .contactPersonMobile(rfq.getContactPersonMobile())
                .status(rfq.getStatus())
                .attachmentUrl(rfq.getAttachmentUrl())
                .createdDate(rfq.getCreatedDate())
                .updatedDate(rfq.getUpdatedDate())
                .createdBy(rfq.getCreatedBy())
                .updatedBy(rfq.getUpdatedBy())
                .deleted(rfq.isDeleted())
                .vendors(mapRFQVendors(rfq.getVendors()))
                .build();
    }




    private List<RFQVendorResponseDto> mapRFQVendors(List<RFQVendor> rfqVendors) {

        List<RFQVendorResponseDto> vendorResponseList = new ArrayList<>();

        if (rfqVendors == null || rfqVendors.isEmpty()) {
            return vendorResponseList;
        }

        for (RFQVendor rfqVendor : rfqVendors) {

            Vendor vendor = rfqVendor.getVendor();

            if (vendor == null) {
                continue;
            }

            RFQVendorResponseDto responseDto = RFQVendorResponseDto.builder()
                    .rfqVendorId(rfqVendor.getId())
                    .vendorId(vendor.getId())
                    .vendorName(vendor.getName())
                    .vendorEmail(vendor.getEmail())
                    .vendorMobile(vendor.getMobile())
                    .gstNumber(vendor.getGstNumber())
                    .panNumber(vendor.getPanNumber())
                    .vendorStatus(vendor.getStatus() != null ? vendor.getStatus().name() : null)
                    .build();

            vendorResponseList.add(responseDto);
        }

        return vendorResponseList;
    }
}