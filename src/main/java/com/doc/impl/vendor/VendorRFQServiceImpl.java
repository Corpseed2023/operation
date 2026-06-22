package com.doc.impl.vendor;

import com.doc.dto.vendor.RFQCreateRequestDto;
import com.doc.dto.vendor.RFQResponseDto;
import com.doc.dto.vendor.RFQUpdateRequestDto;
import com.doc.dto.vendor.RFQVendorResponseDto;
import com.doc.entity.product.Product;
import com.doc.entity.vendor.RFQ;
import com.doc.entity.vendor.RFQStatus;
import com.doc.entity.vendor.RFQVendor;
import com.doc.entity.vendor.Vendor;
import com.doc.repository.ProductRepository;
import com.doc.repository.vendor.VendorRFQRepository;
import com.doc.repository.vendor.VendorRepository;
import com.doc.service.vendor.VendorRFQService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class VendorRFQServiceImpl implements VendorRFQService {

    private final VendorRFQRepository vendorRFQRepository;
    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;

    @Override
    @Transactional
    public RFQResponseDto createRFQ(Long userId, RFQCreateRequestDto requestDto) {

        Product product = productRepository.findById(requestDto.getProductId())
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
    public Page<RFQResponseDto> getAllRFQs(
            Long productId,
            RFQStatus status,
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

        return vendorRFQRepository.getAllRFQs(productId, status, pageable);
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

    private RFQResponseDto mapToResponseDto(RFQ rfq) {

        Product product = rfq.getProduct();

        RFQResponseDto responseDto = new RFQResponseDto(
                rfq.getId(),
                rfq.getRfqNumber(),
                rfq.getTitle(),
                rfq.getDescription(),
                product != null ? product.getId() : null,
                product != null ? product.getProductName() : null,
                rfq.getScopeOfWork(),
                rfq.getTermsAndConditions(),
                rfq.getDeliveryLocation(),
                rfq.getQuotationSubmissionDeadline(),
                rfq.getExpectedStartDate(),
                rfq.getExpectedEndDate(),
                rfq.getContactPersonName(),
                rfq.getContactPersonEmail(),
                rfq.getContactPersonMobile(),
                rfq.getStatus(),
                rfq.getAttachmentUrl(),
                rfq.getCreatedDate(),
                rfq.getUpdatedDate(),
                rfq.getCreatedBy(),
                rfq.getUpdatedBy(),
                rfq.isDeleted()
        );

        responseDto.setVendors(mapRFQVendors(rfq.getVendors()));

        return responseDto;
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