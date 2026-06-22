package com.doc.impl.vendor;

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

@Service
public class VendorServiceImpl implements VendorService {

    private static final Logger logger = LoggerFactory.getLogger(VendorServiceImpl.class);

    private final VendorRepository vendorRepository;
    private final VendorMailService vendorMailService;
    private final UserRepository userRepository;

    public VendorServiceImpl(
            VendorRepository vendorRepository,
            VendorMailService vendorMailService,
            UserRepository userRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorMailService = vendorMailService;
        this.userRepository = userRepository;
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


}