package com.doc.impl.vendor;

import com.doc.dto.vendor.NewVendorDto;
import com.doc.dto.vendor.ProductVendorCreateRequestDto;
import com.doc.dto.vendor.ProductVendorResponseDto;
import com.doc.dto.vendor.ProductVendorUpdateRequestDto;
import com.doc.entity.product.Product;
import com.doc.entity.user.User;
import com.doc.entity.vendor.ProductVendorMapping;
import com.doc.entity.vendor.Vendor;
import com.doc.entity.vendor.VendorStatus;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProductRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.vendor.ProductVendorMappingRepository;
import com.doc.repository.vendor.VendorRepository;
import com.doc.service.vendor.ProductVendorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class ProductVendorServiceImpl implements ProductVendorService {

    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final ProductVendorMappingRepository productVendorMappingRepository;
    private final UserRepository userRepository;

    public ProductVendorServiceImpl(
            ProductRepository productRepository,
            VendorRepository vendorRepository,
            ProductVendorMappingRepository productVendorMappingRepository,
            UserRepository userRepository
    ) {
        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
        this.productVendorMappingRepository = productVendorMappingRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ProductVendorResponseDto createVendorAgainstProduct(
            Long productId,
            Long userId,
            ProductVendorCreateRequestDto dto
    ) {
        User currentUser = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        Product product = productRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found",
                        "ERR_PRODUCT_NOT_FOUND"
                ));

        Vendor vendor;

        /*
         * CASE 1: Existing vendor selected from dropdown.
         */
        if (dto.getVendorId() != null) {

            vendor = vendorRepository.findByIdAndIsDeletedFalse(dto.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Vendor not found",
                            "ERR_VENDOR_NOT_FOUND"
                    ));

        } else {

            /*
             * CASE 2: New vendor created from product screen.
             */
            if (dto.getVendor() == null) {
                throw new ValidationException(
                        "Either vendorId or vendor details are required",
                        "ERR_VENDOR_REQUIRED"
                );
            }

            validateNewVendor(dto.getVendor());

            vendor = new Vendor();
            vendor.setName(dto.getVendor().getName().trim());
            vendor.setDescription(dto.getVendor().getDescription());
            vendor.setEmail(dto.getVendor().getEmail());
            vendor.setMobile(dto.getVendor().getMobile());
            vendor.setGstNumber(normalize(dto.getVendor().getGstNumber()));
            vendor.setPanNumber(normalize(dto.getVendor().getPanNumber()));
            vendor.setStatus(dto.getVendor().getStatus() != null
                    ? dto.getVendor().getStatus()
                    : VendorStatus.ACTIVE);
            vendor.setVerified(dto.getVendor().isVerified());
            vendor.setCreatedBy(currentUser.getId());
            vendor.setUpdatedBy(currentUser.getId());
            vendor.setCreatedDate(new Date());
            vendor.setUpdatedDate(new Date());
            vendor.setDeleted(false);

            vendor = vendorRepository.save(vendor);
        }

        /*
         * Prevent same vendor from being mapped again with same product.
         * Example: CDSO + Ramesh Trader should not be duplicated.
         */
        if (productVendorMappingRepository.existsByProductIdAndVendorIdAndIsDeletedFalse(
                product.getId(),
                vendor.getId()
        )) {
            throw new ValidationException(
                    "This vendor is already mapped with this product",
                    "ERR_PRODUCT_VENDOR_ALREADY_EXISTS"
            );
        }

        ProductVendorMapping mapping = new ProductVendorMapping();
        mapping.setProduct(product);
        mapping.setVendor(vendor);

        mapping.setEmailSubject(normalize(dto.getEmailSubject()));
        mapping.setEmailBody(normalize(dto.getEmailBody()));
        mapping.setAgreementAttachment(normalize(dto.getAgreementAttachment()));

        mapping.setCreatedBy(currentUser.getId());
        mapping.setUpdatedBy(currentUser.getId());
        mapping.setCreatedDate(new Date());
        mapping.setUpdatedDate(new Date());
        mapping.setActive(true);
        mapping.setDeleted(false);

        mapping = productVendorMappingRepository.save(mapping);

        return mapToResponse(mapping);
    }

    private void validateNewVendor(NewVendorDto vendorDto) {

        if (vendorDto.getName() == null || vendorDto.getName().trim().isEmpty()) {
            throw new ValidationException(
                    "Vendor name is required",
                    "ERR_VENDOR_NAME_REQUIRED"
            );
        }

        String gstNumber = normalize(vendorDto.getGstNumber());

        if (gstNumber != null &&
                vendorRepository.existsByGstNumberAndIsDeletedFalse(gstNumber)) {
            throw new ValidationException(
                    "GST number already exists",
                    "ERR_DUPLICATE_GST"
            );
        }

        String panNumber = normalize(vendorDto.getPanNumber());

        if (panNumber != null &&
                vendorRepository.existsByPanNumberAndIsDeletedFalse(panNumber)) {
            throw new ValidationException(
                    "PAN number already exists",
                    "ERR_DUPLICATE_PAN"
            );
        }
    }
    @Override
    public Page<ProductVendorResponseDto> getVendorsByProduct(
            Long productId,
            Long userId,
            int page,
            int size
    ) {
        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        productRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found",
                        "ERR_PRODUCT_NOT_FOUND"
                ));

        int pageIndex = page <= 0 ? 0 : page - 1;
        int pageSize = size <= 0 ? 10 : size;

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by("createdDate").descending()
        );

        return productVendorMappingRepository
                .findByProductIdAndIsDeletedFalse(productId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public ProductVendorResponseDto updateProductVendorMapping(
            Long mappingId,
            Long userId,
            ProductVendorUpdateRequestDto dto
    ) {
        User currentUser = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        ProductVendorMapping mapping = productVendorMappingRepository.findByIdAndIsDeletedFalse(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product vendor mapping not found",
                        "ERR_PRODUCT_VENDOR_MAPPING_NOT_FOUND"
                ));

        /*
         * Optional: change vendor in this mapping.
         * Example: mapping was CDSO + Balaji Trader,
         * now user wants CDSO + Vishu Trader.
         */
        if (dto.getVendorId() != null) {

            Long currentVendorId = mapping.getVendor() != null
                    ? mapping.getVendor().getId()
                    : null;

            if (!dto.getVendorId().equals(currentVendorId)) {

                Vendor newVendor = vendorRepository.findByIdAndIsDeletedFalse(dto.getVendorId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Vendor not found",
                                "ERR_VENDOR_NOT_FOUND"
                        ));

                Long productId = mapping.getProduct().getId();

                if (productVendorMappingRepository.existsByProductIdAndVendorIdAndIsDeletedFalse(
                        productId,
                        newVendor.getId()
                )) {
                    throw new ValidationException(
                            "This vendor is already mapped with this product",
                            "ERR_PRODUCT_VENDOR_ALREADY_EXISTS"
                    );
                }

                mapping.setVendor(newVendor);
            }
        }

        /*
         * Mapping-specific fields only.
         * Do not update vendor master data here.
         */
        if (dto.getEmailSubject() != null) {
            mapping.setEmailSubject(normalize(dto.getEmailSubject()));
        }

        if (dto.getEmailBody() != null) {
            mapping.setEmailBody(normalize(dto.getEmailBody()));
        }

        if (dto.getAgreementAttachment() != null) {
            mapping.setAgreementAttachment(normalize(dto.getAgreementAttachment()));
        }

        mapping.setUpdatedBy(currentUser.getId());
        mapping.setUpdatedDate(new Date());

        mapping = productVendorMappingRepository.save(mapping);

        return mapToResponse(mapping);
    }

    @Override
    public void removeVendorFromProduct(Long mappingId, Long userId) {
        User currentUser = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        ProductVendorMapping mapping = productVendorMappingRepository.findByIdAndIsDeletedFalse(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product vendor mapping not found",
                        "ERR_PRODUCT_VENDOR_MAPPING_NOT_FOUND"
                ));

        // Only remove mapping, not vendor master.
        mapping.setDeleted(true);
        mapping.setActive(false);
        mapping.setUpdatedBy(currentUser.getId());
        mapping.setUpdatedDate(new Date());

        productVendorMappingRepository.save(mapping);
    }


    private String normalize(String value) {
        return value != null && !value.trim().isEmpty()
                ? value.trim()
                : null;
    }

    private ProductVendorResponseDto mapToResponse(ProductVendorMapping mapping) {
        ProductVendorResponseDto dto = new ProductVendorResponseDto();

        Product product = mapping.getProduct();
        Vendor vendor = mapping.getVendor();

        dto.setMappingId(mapping.getId());

        dto.setProductId(product != null ? product.getId() : null);
        dto.setProductName(product != null ? product.getProductName() : null);

        dto.setVendorId(vendor != null ? vendor.getId() : null);
        dto.setVendorName(vendor != null ? vendor.getName() : null);
        dto.setDescription(vendor != null ? vendor.getDescription() : null);
        dto.setEmail(vendor != null ? vendor.getEmail() : null);
        dto.setMobile(vendor != null ? vendor.getMobile() : null);
        dto.setGstNumber(vendor != null ? vendor.getGstNumber() : null);
        dto.setPanNumber(vendor != null ? vendor.getPanNumber() : null);
        dto.setStatus(vendor != null ? vendor.getStatus() : null);
        dto.setVerified(vendor != null && vendor.isVerified());

        dto.setEmailSubject(mapping.getEmailSubject());
        dto.setEmailBody(mapping.getEmailBody());
        dto.setAgreementAttachment(mapping.getAgreementAttachment());

        dto.setActive(mapping.isActive());
        dto.setDeleted(mapping.isDeleted());

        dto.setCreatedBy(mapping.getCreatedBy());
        dto.setUpdatedBy(mapping.getUpdatedBy());
        dto.setCreatedDate(mapping.getCreatedDate());
        dto.setUpdatedDate(mapping.getUpdatedDate());

        return dto;
    }
}