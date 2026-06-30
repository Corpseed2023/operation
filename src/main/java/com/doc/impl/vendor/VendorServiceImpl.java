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
import com.doc.repository.vendor.VendorRepository;
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

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.doc.repository.vendor.RFQVendorRepository;
import com.doc.repository.vendor.VendorQuotationRepository;
import com.doc.repository.vendor.VendorFinalizationRepository;
import com.doc.repository.vendor.VendorOnboardingRepository;
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

    public VendorServiceImpl(
            VendorRepository vendorRepository,
            VendorMailService vendorMailService,
            UserRepository userRepository,
            RFQVendorRepository rfqVendorRepository,
            VendorQuotationRepository vendorQuotationRepository,
            VendorFinalizationRepository vendorFinalizationRepository,
            VendorOnboardingRepository vendorOnboardingRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorMailService = vendorMailService;
        this.userRepository = userRepository;
        this.rfqVendorRepository = rfqVendorRepository;
        this.vendorQuotationRepository = vendorQuotationRepository;
        this.vendorFinalizationRepository = vendorFinalizationRepository;
        this.vendorOnboardingRepository = vendorOnboardingRepository;
    }

    @Override
    @Transactional
    public VendorResponseDto createVendor(Long userId, VendorRequestDto dto) {

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ValidationException("Vendor name is required", "ERR_VENDOR_NAME_REQUIRED");
        }

        String gstNumber = normalize(dto.getGstNumber());
        String panNumber = normalize(dto.getPanNumber());

        if (gstNumber != null &&
                vendorRepository.existsByGstNumberAndIsDeletedFalse(gstNumber)) {
            throw new ValidationException("GST number already exists", "ERR_DUPLICATE_GST");
        }

        if (panNumber != null &&
                vendorRepository.existsByPanNumberAndIsDeletedFalse(panNumber)) {
            throw new ValidationException("PAN number already exists", "ERR_DUPLICATE_PAN");
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
        vendor.setGstNumber(gstNumber);
        vendor.setPanNumber(panNumber);
        vendor.setStatus(VendorStatus.PROSPECTIVE);
        vendor.setCreatedBy(createdByUser.getId());
        vendor.setUpdatedBy(createdByUser.getId());
        vendor.setDeleted(false);

        vendor = vendorRepository.save(vendor);

        logger.info("Vendor created successfully with ID: {}", vendor.getId());

        return mapEntityToDto(vendor);
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
    public Page<VendorResponseDto> getAllVendors(Long userId, int page, int size, String keyword) {

        // Validate user
        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "ERR_USER_NOT_FOUND"));

        // Frontend page starts from 1, Spring page starts from 0
        int pageIndex = page <= 0 ? 0 : page - 1;

        // Optional safety for size
        int pageSize = size <= 0 ? 10 : size;

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by("createdDate").descending()
        );

        Page<Vendor> vendors;

        if (keyword != null && !keyword.trim().isEmpty()) {
            vendors = vendorRepository.searchVendors(keyword.trim(), pageable);
        } else {
            vendors = vendorRepository.findByIsDeletedFalse(pageable);
        }

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
}