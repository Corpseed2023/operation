package com.doc.impl.vendor;

import com.doc.dto.vendor.ProductVendorCreateRequestDto;
import com.doc.dto.vendor.ProductVendorResponseDto;
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

        if (dto.getExistingVendorId() != null) {
            vendor = vendorRepository.findByIdAndIsDeletedFalse(dto.getExistingVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Vendor not found",
                            "ERR_VENDOR_NOT_FOUND"
                    ));
        } else {
            validateNewVendor(dto);

            vendor = new Vendor();
            vendor.setName(dto.getName().trim());
            vendor.setDescription(dto.getDescription());
            vendor.setEmail(dto.getEmail());
            vendor.setMobile(dto.getMobile());
            vendor.setGstNumber(normalize(dto.getGstNumber()));
            vendor.setPanNumber(normalize(dto.getPanNumber()));
            vendor.setStatus(dto.getStatus() != null ? dto.getStatus() : VendorStatus.ACTIVE);
            vendor.setVerified(dto.isVerified());
            vendor.setCreatedBy(currentUser.getId());
            vendor.setUpdatedBy(currentUser.getId());
            vendor.setCreatedDate(new Date());
            vendor.setUpdatedDate(new Date());
            vendor.setDeleted(false);

            vendor = vendorRepository.save(vendor);
        }

        if (productVendorMappingRepository.existsByProductIdAndVendorIdAndIsDeletedFalse(product.getId(), vendor.getId())) {
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
            ProductVendorCreateRequestDto dto
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

        Vendor vendor = mapping.getVendor();

        // Vendor master fields update
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            vendor.setName(dto.getName().trim());
        }

        if (dto.getDescription() != null) {
            vendor.setDescription(dto.getDescription());
        }

        if (dto.getEmail() != null) {
            vendor.setEmail(dto.getEmail());
        }

        if (dto.getMobile() != null) {
            vendor.setMobile(dto.getMobile());
        }

        if (dto.getStatus() != null) {
            vendor.setStatus(dto.getStatus());
        }

        vendor.setVerified(dto.isVerified());
        vendor.setUpdatedBy(currentUser.getId());
        vendor.setUpdatedDate(new Date());

        vendorRepository.save(vendor);

        // Mapping-specific fields update
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

    private void validateNewVendor(ProductVendorCreateRequestDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ValidationException(
                    "Vendor name is required",
                    "ERR_VENDOR_NAME_REQUIRED"
            );
        }

        String gstNumber = normalize(dto.getGstNumber());
        if (gstNumber != null && vendorRepository.existsByGstNumberAndIsDeletedFalse(gstNumber)) {
            throw new ValidationException(
                    "GST number already exists",
                    "ERR_DUPLICATE_GST"
            );
        }

        String panNumber = normalize(dto.getPanNumber());
        if (panNumber != null && vendorRepository.existsByPanNumberAndIsDeletedFalse(panNumber)) {
            throw new ValidationException(
                    "PAN number already exists",
                    "ERR_DUPLICATE_PAN"
            );
        }
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