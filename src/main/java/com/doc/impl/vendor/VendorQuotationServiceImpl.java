package com.doc.impl.vendor;

import com.doc.dto.vendor.VendorQuotationItemRequestDto;
import com.doc.dto.vendor.VendorQuotationItemResponseDto;
import com.doc.dto.vendor.VendorQuotationRequestDto;
import com.doc.dto.vendor.VendorQuotationResponseDto;
import com.doc.entity.vendor.RFQ;
import com.doc.entity.vendor.RFQVendor;
import com.doc.entity.vendor.Vendor;
import com.doc.entity.vendor.VendorQuotation;
import com.doc.entity.vendor.VendorQuotationItem;
import com.doc.repository.vendor.RFQVendorRepository;
import com.doc.repository.vendor.VendorQuotationRepository;
import com.doc.repository.vendor.VendorRFQRepository;
import com.doc.repository.vendor.VendorRepository;
import com.doc.service.vendor.VendorQuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorQuotationServiceImpl implements VendorQuotationService {

    private final VendorQuotationRepository vendorQuotationRepository;
    private final VendorRFQRepository rfqRepository;
    private final RFQVendorRepository rfqVendorRepository;
    private final VendorRepository vendorRepository;

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

        VendorQuotation quotation = new VendorQuotation();

        quotation.setRfq(rfq);
        quotation.setRfqVendor(rfqVendor);
        quotation.setVendor(vendor);

        quotation.setQuotationNumber(requestDto.getQuotationNumber());
        quotation.setQuotationDate(requestDto.getQuotationDate());
        quotation.setValidTill(requestDto.getValidTill());

        quotation.setCurrency(requestDto.getCurrency() != null ? requestDto.getCurrency() : "INR");
        quotation.setDeliveryDays(requestDto.getDeliveryDays());

        quotation.setPaymentTerms(requestDto.getPaymentTerms());
        quotation.setWarrantyTerms(requestDto.getWarrantyTerms());
        quotation.setRemarks(requestDto.getRemarks());
        quotation.setQuotationAttachmentUrl(requestDto.getQuotationAttachmentUrl());

        quotation.setCreatedBy(requestDto.getCreatedBy());

        if (requestDto.getItems() != null) {
            for (VendorQuotationItemRequestDto itemDto : requestDto.getItems()) {

                VendorQuotationItem item = new VendorQuotationItem();

                item.setItemType(itemDto.getItemType());
//                item.setSequenceNo(itemDto.getSequenceNo());
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

        return mapToResponse(saved);
    }


    private VendorQuotationResponseDto mapToResponse(VendorQuotation quotation) {

        VendorQuotationResponseDto response = new VendorQuotationResponseDto();

        response.setId(quotation.getId());
        response.setQuotationNumber(quotation.getQuotationNumber());

        response.setRfqId(quotation.getRfq() != null ? quotation.getRfq().getId() : null);
        response.setRfqVendorId(quotation.getRfqVendor() != null ? quotation.getRfqVendor().getId() : null);

        if (quotation.getVendor() != null) {
            response.setVendorId(quotation.getVendor().getId());
            response.setVendorName(quotation.getVendor().getName());
            response.setVendorEmail(quotation.getVendor().getEmail());
            response.setVendorMobile(quotation.getVendor().getMobile());
        }

        response.setQuotationDate(quotation.getQuotationDate());
        response.setValidTill(quotation.getValidTill());

        response.setVersionNo(quotation.getVersionNo());
        response.setLatest(quotation.isLatest());
        response.setCurrency(quotation.getCurrency());

        response.setSubtotalAmount(quotation.getSubtotalAmount());
        response.setTaxAmount(quotation.getTaxAmount());
        response.setGrandTotal(quotation.getGrandTotal());

        response.setDeliveryDays(quotation.getDeliveryDays());
        response.setPaymentTerms(quotation.getPaymentTerms());
        response.setWarrantyTerms(quotation.getWarrantyTerms());
        response.setRemarks(quotation.getRemarks());
        response.setQuotationAttachmentUrl(quotation.getQuotationAttachmentUrl());

        response.setStatus(quotation.getStatus() != null ? quotation.getStatus().name() : null);

        response.setCreatedBy(quotation.getCreatedBy());
        response.setUpdatedBy(quotation.getUpdatedBy());
        response.setCreatedDate(quotation.getCreatedDate());
        response.setUpdatedDate(quotation.getUpdatedDate());
        response.setDeleted(quotation.isDeleted());

        List<VendorQuotationItemResponseDto> itemResponses = new ArrayList<>();

        if (quotation.getItems() != null) {
            for (VendorQuotationItem item : quotation.getItems()) {

                VendorQuotationItemResponseDto itemResponse = new VendorQuotationItemResponseDto();

                itemResponse.setId(item.getId());
                itemResponse.setQuotationId(quotation.getId());
                itemResponse.setItemType(item.getItemType() != null ? item.getItemType().name() : null);
//                itemResponse.setSequenceNo(item.getSequenceNo());
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
    public List<VendorQuotationResponseDto> getAllVendorQuotations() {

        List<VendorQuotation> quotations =
                vendorQuotationRepository.findByIsDeletedFalseOrderByCreatedDateDesc();

        List<VendorQuotationResponseDto> responseList = new ArrayList<>();

        for (VendorQuotation quotation : quotations) {
            responseList.add(mapToResponse(quotation));
        }

        return responseList;
    }
}