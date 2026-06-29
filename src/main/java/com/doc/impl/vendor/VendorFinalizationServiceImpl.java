package com.doc.impl.vendor;

import com.doc.dto.vendor.AccountsVendorFinalizationRequestDto;
import com.doc.dto.vendor.SendFinalVendorToAccountsRequestDto;
import com.doc.dto.vendor.VendorFinalizationRequestDto;
import com.doc.dto.vendor.VendorFinalizationResponseDto;
import com.doc.entity.vendor.*;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.vendor.*;
import com.doc.service.vendor.VendorFinalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorFinalizationServiceImpl implements VendorFinalizationService {

    private final VendorFinalizationRepository vendorFinalizationRepository;
    private final VendorRFQRepository vendorRFQRepository;
    private final RFQVendorRepository rfqVendorRepository;
    private final VendorRepository vendorRepository;
    private final VendorQuotationRepository vendorQuotationRepository;
    private final VendorQuotationItemRepository vendorQuotationItemRepository;

    @Override
    @Transactional
    public VendorFinalizationResponseDto createVendorFinalization(
            VendorFinalizationRequestDto requestDto
    ) {
        RFQ rfq = vendorRFQRepository.findById(requestDto.getRfqId())
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        if (rfq.isDeleted()) {
            throw new RuntimeException("RFQ is deleted");
        }

        RFQVendor rfqVendor = rfqVendorRepository.findById(requestDto.getRfqVendorId())
                .orElseThrow(() -> new RuntimeException("RFQ vendor not found"));

        Vendor vendor = vendorRepository.findById(requestDto.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        VendorQuotation quotation = vendorQuotationRepository.findById(requestDto.getQuotationId())
                .orElseThrow(() -> new RuntimeException("Vendor quotation not found"));

        VendorQuotationItem quotationItem = vendorQuotationItemRepository
                .findByIdAndIsDeletedFalse(requestDto.getQuotationItemId())
                .orElseThrow(() -> new RuntimeException("Vendor quotation item not found"));

        if (!rfqVendor.getRfq().getId().equals(rfq.getId())) {
            throw new RuntimeException("RFQ vendor does not belong to selected RFQ");
        }

        if (!rfqVendor.getVendor().getId().equals(vendor.getId())) {
            throw new RuntimeException("Vendor does not match RFQ vendor");
        }

        if (!quotation.getRfq().getId().equals(rfq.getId())) {
            throw new RuntimeException("Quotation does not belong to selected RFQ");
        }

        if (!quotation.getRfqVendor().getId().equals(rfqVendor.getId())) {
            throw new RuntimeException("Quotation does not belong to selected RFQ vendor");
        }

        if (!quotation.getVendor().getId().equals(vendor.getId())) {
            throw new RuntimeException("Quotation does not belong to selected vendor");
        }

        if (!quotationItem.getQuotation().getId().equals(quotation.getId())) {
            throw new RuntimeException("Quotation item does not belong to selected quotation");
        }

        boolean alreadyFinalized =
                vendorFinalizationRepository.existsByRfqVendor_IdAndQuotationItem_IdAndIsDeletedFalse(
                        requestDto.getRfqVendorId(),
                        requestDto.getQuotationItemId()
                );

        if (alreadyFinalized) {
            throw new RuntimeException("This quotation item is already finalized for this RFQ vendor");
        }

        BigDecimal quantity = requestDto.getFinalizedQuantity();
        BigDecimal unitRate = requestDto.getFinalizedUnitRate();
        BigDecimal taxPercent = requestDto.getTaxPercent() != null
                ? requestDto.getTaxPercent()
                : BigDecimal.ZERO;

        BigDecimal finalizedAmount = quantity.multiply(unitRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxAmount = finalizedAmount
                .multiply(taxPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalFinalizedAmount = finalizedAmount.add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        VendorFinalization finalization = new VendorFinalization();

        finalization.setRfq(rfq);
        finalization.setRfqVendor(rfqVendor);
        finalization.setVendor(vendor);
        finalization.setQuotation(quotation);
        finalization.setQuotationItem(quotationItem);

        finalization.setDescription(requestDto.getDescription());
        finalization.setFinalizedQuantity(quantity);
        finalization.setUnit(requestDto.getUnit());
        finalization.setFinalizedUnitRate(unitRate);
        finalization.setFinalizedAmount(finalizedAmount);
        finalization.setTaxPercent(taxPercent);
        finalization.setTaxAmount(taxAmount);
        finalization.setTotalFinalizedAmount(totalFinalizedAmount);

        finalization.setFinalizationReason(requestDto.getFinalizationReason());
        finalization.setRemarks(requestDto.getRemarks());

        finalization.setStatus(VendorFinalizationStatus.FINALIZED);
        finalization.setFinalizedBy(requestDto.getCreatedBy());
        finalization.setFinalizedDate(new Date());

        finalization.setCreatedBy(requestDto.getCreatedBy());
        finalization.setUpdatedBy(requestDto.getCreatedBy());
        finalization.setDeleted(false);

        rfqVendor.setStatus(RFQVendorStatus.SELECTED);
        rfqVendor.setUpdatedBy(requestDto.getCreatedBy());
        rfqVendor.setRemarks("Vendor finalized for quotation item");

        VendorFinalization saved = vendorFinalizationRepository.save(finalization);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorFinalizationResponseDto getVendorFinalizationById(Long id) {
        VendorFinalization finalization = vendorFinalizationRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Vendor finalization not found"));

        return mapToResponse(finalization);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorFinalizationResponseDto> getVendorFinalizationsByRfqId(Long rfqId) {
        List<VendorFinalization> finalizations =
                vendorFinalizationRepository.findByRfq_IdAndIsDeletedFalseOrderByCreatedDateDesc(rfqId);

        List<VendorFinalizationResponseDto> responseList = new ArrayList<>();

        for (VendorFinalization finalization : finalizations) {
            responseList.add(mapToResponse(finalization));
        }

        return responseList;
    }

    @Override
    @Transactional
    public VendorFinalizationResponseDto sendToAccounts(
            Long finalizationId,
            SendFinalVendorToAccountsRequestDto requestDto
    ) {
        VendorFinalization finalization = vendorFinalizationRepository
                .findByIdAndIsDeletedFalse(finalizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor finalization not found",
                        "ERR_VENDOR_FINALIZATION_NOT_FOUND"
                ));

        if (finalization.isSentToAccounts()) {
            throw new ValidationException(
                    "Final vendor details already sent to accounts",
                    "ERR_ALREADY_SENT_TO_ACCOUNTS"
            );
        }

        finalization.setFinalVendorAttachmentUrl(
                requestDto.getFinalVendorAttachmentUrl()
        );
        finalization.setFinalVendorRemarks(requestDto.getFinalVendorRemarks());
        finalization.setSentToAccounts(true);
        finalization.setSentToAccountsBy(requestDto.getUserId());
        finalization.setSentToAccountsDate(new Date());
        finalization.setUpdatedBy(requestDto.getUserId());

        VendorFinalization saved = vendorFinalizationRepository.save(finalization);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorFinalizationResponseDto> getAllSentToAccounts() {
        return vendorFinalizationRepository
                .findBySentToAccountsTrueAndIsDeletedFalseOrderBySentToAccountsDateDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private VendorFinalizationResponseDto mapToResponse(VendorFinalization finalization) {
        VendorFinalizationResponseDto response = new VendorFinalizationResponseDto();

        response.setId(finalization.getId());

        if (finalization.getRfq() != null) {
            response.setRfqId(finalization.getRfq().getId());
            response.setRfqNumber(finalization.getRfq().getRfqNumber());
        }

        if (finalization.getRfqVendor() != null) {
            response.setRfqVendorId(finalization.getRfqVendor().getId());
        }

        if (finalization.getVendor() != null) {
            response.setVendorId(finalization.getVendor().getId());
            response.setVendorName(finalization.getVendor().getName());
            response.setVendorEmail(finalization.getVendor().getEmail());
            response.setVendorMobile(finalization.getVendor().getMobile());
        }

        if (finalization.getQuotation() != null) {
            response.setQuotationId(finalization.getQuotation().getId());
            response.setQuotationNumber(finalization.getQuotation().getQuotationNumber());
        }

        if (finalization.getQuotationItem() != null) {
            response.setQuotationItemId(finalization.getQuotationItem().getId());
            response.setQuotationItemName(finalization.getQuotationItem().getItemName());
        }

        response.setDescription(finalization.getDescription());
        response.setFinalizedQuantity(finalization.getFinalizedQuantity());
        response.setUnit(finalization.getUnit());
        response.setFinalizedUnitRate(finalization.getFinalizedUnitRate());
        response.setFinalizedAmount(finalization.getFinalizedAmount());
        response.setTaxPercent(finalization.getTaxPercent());
        response.setTaxAmount(finalization.getTaxAmount());
        response.setTotalFinalizedAmount(finalization.getTotalFinalizedAmount());

        response.setFinalizationReason(finalization.getFinalizationReason());
        response.setRemarks(finalization.getRemarks());

        response.setStatus(
                finalization.getStatus() != null ? finalization.getStatus().name() : null
        );

        response.setFinalizedBy(finalization.getFinalizedBy());
        response.setFinalizedDate(finalization.getFinalizedDate());

        response.setCreatedBy(finalization.getCreatedBy());
        response.setUpdatedBy(finalization.getUpdatedBy());
        response.setCreatedDate(finalization.getCreatedDate());
        response.setUpdatedDate(finalization.getUpdatedDate());
        response.setDeleted(finalization.isDeleted());

        response.setFinalVendorAttachmentUrl(finalization.getFinalVendorAttachmentUrl());
        response.setFinalVendorRemarks(finalization.getFinalVendorRemarks());
        response.setSentToAccounts(finalization.isSentToAccounts());
        response.setSentToAccountsBy(finalization.getSentToAccountsBy());
        response.setSentToAccountsDate(finalization.getSentToAccountsDate());

        return response;
    }

    @Override
    @Transactional
    public VendorFinalizationResponseDto approveByAccounts(
            Long finalizationId,
            AccountsVendorFinalizationRequestDto requestDto
    ) {
        VendorFinalization finalization = vendorFinalizationRepository
                .findByIdAndIsDeletedFalse(finalizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor finalization not found",
                        "ERR_VENDOR_FINALIZATION_NOT_FOUND"
                ));

        if (!finalization.isSentToAccounts()) {
            throw new ValidationException(
                    "Final vendor details are not sent to accounts yet",
                    "ERR_NOT_SENT_TO_ACCOUNTS"
            );
        }

        if (finalization.getStatus() == VendorFinalizationStatus.ACCOUNTS_APPROVED) {
            throw new ValidationException(
                    "Final vendor already approved by accounts",
                    "ERR_ALREADY_APPROVED_BY_ACCOUNTS"
            );
        }

        if (finalization.getStatus() == VendorFinalizationStatus.ACCOUNTS_REJECTED) {
            throw new ValidationException(
                    "Rejected final vendor cannot be approved directly",
                    "ERR_ALREADY_REJECTED_BY_ACCOUNTS"
            );
        }

        if (finalization.getStatus() != VendorFinalizationStatus.SENT_TO_ACCOUNTS) {
            throw new ValidationException(
                    "Only vendor sent to accounts can be approved",
                    "ERR_INVALID_FINALIZATION_STATUS"
            );
        }

        finalization.setStatus(VendorFinalizationStatus.ACCOUNTS_APPROVED);
        finalization.setAccountsRemark(requestDto.getAccountsRemark());
        finalization.setAccountsVerifiedBy(requestDto.getUserId());
        finalization.setAccountsVerifiedDate(new Date());
        finalization.setUpdatedBy(requestDto.getUserId());

        VendorFinalization saved = vendorFinalizationRepository.save(finalization);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public VendorFinalizationResponseDto rejectByAccounts(
            Long finalizationId,
            AccountsVendorFinalizationRequestDto requestDto
    ) {
        VendorFinalization finalization = vendorFinalizationRepository
                .findByIdAndIsDeletedFalse(finalizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor finalization not found",
                        "ERR_VENDOR_FINALIZATION_NOT_FOUND"
                ));

        if (!finalization.isSentToAccounts()) {
            throw new ValidationException(
                    "Final vendor details are not sent to accounts yet",
                    "ERR_NOT_SENT_TO_ACCOUNTS"
            );
        }

        if (finalization.getStatus() == VendorFinalizationStatus.ACCOUNTS_APPROVED) {
            throw new ValidationException(
                    "Approved final vendor cannot be rejected",
                    "ERR_ALREADY_APPROVED_BY_ACCOUNTS"
            );
        }

        if (finalization.getStatus() == VendorFinalizationStatus.ACCOUNTS_REJECTED) {
            throw new ValidationException(
                    "Final vendor already rejected by accounts",
                    "ERR_ALREADY_REJECTED_BY_ACCOUNTS"
            );
        }

        if (finalization.getStatus() != VendorFinalizationStatus.SENT_TO_ACCOUNTS) {
            throw new ValidationException(
                    "Only vendor sent to accounts can be rejected",
                    "ERR_INVALID_FINALIZATION_STATUS"
            );
        }

        finalization.setStatus(VendorFinalizationStatus.ACCOUNTS_REJECTED);
        finalization.setAccountsRemark(requestDto.getAccountsRemark());
        finalization.setAccountsVerifiedBy(requestDto.getUserId());
        finalization.setAccountsVerifiedDate(new Date());
        finalization.setUpdatedBy(requestDto.getUserId());

        VendorFinalization saved = vendorFinalizationRepository.save(finalization);

        return mapToResponse(saved);
    }



}