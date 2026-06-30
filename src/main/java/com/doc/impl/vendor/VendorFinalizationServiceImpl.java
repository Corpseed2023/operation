package com.doc.impl.vendor;

import com.doc.dto.vendor.*;
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
    private final VendorAccountsSubmissionRepository vendorAccountsSubmissionRepository;

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
    public VendorAccountsSubmissionResponseDto sendToAccounts(
            Long finalizationId,
            VendorAccountsSubmissionRequestDto requestDto
    ) {
        VendorFinalization finalization = vendorFinalizationRepository
                .findByIdAndIsDeletedFalse(finalizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor finalization not found",
                        "ERR_VENDOR_FINALIZATION_NOT_FOUND"
                ));

        if (vendorAccountsSubmissionRepository
                .existsByVendorFinalization_IdAndIsDeletedFalse(finalizationId)) {
            throw new ValidationException(
                    "Vendor already sent to accounts",
                    "ERR_VENDOR_ALREADY_SENT_TO_ACCOUNTS"
            );
        }

        VendorAccountsSubmission submission = new VendorAccountsSubmission();

        submission.setVendorFinalization(finalization);
        submission.setVendor(finalization.getVendor());
        submission.setRfq(finalization.getRfq());
        submission.setQuotation(finalization.getQuotation());

        submission.setName(requestDto.getName());
        submission.setNumber(requestDto.getNumber());
        submission.setEmail(requestDto.getEmail());
        submission.setAadhar(requestDto.getAadhar());

        submission.setAuthorizedSignatoryName(requestDto.getAuthorizedSignatoryName());
        submission.setAuthorizedSignatoryNumber(requestDto.getAuthorizedSignatoryNumber());
        submission.setAuthorizedSignatoryEmail(requestDto.getAuthorizedSignatoryEmail());
        submission.setAuthorizedSignatoryAadhar(requestDto.getAuthorizedSignatoryAadhar());

        submission.setAccountHolderName(requestDto.getAccountHolderName());
        submission.setAccountNumber(requestDto.getAccountNumber());
        submission.setIfsc(requestDto.getIfsc());
        submission.setSwiftCode(requestDto.getSwiftCode());
        submission.setBranchAddress(requestDto.getBranchAddress());

        submission.setGstDetailsUrl(requestDto.getGstDetailsUrl());
        submission.setVendorSetupFormUrl(requestDto.getVendorSetupFormUrl());
        submission.setCancelChequeUrl(requestDto.getCancelChequeUrl());
        submission.setItrLastFinancialYearUrl(requestDto.getItrLastFinancialYearUrl());
        submission.setPanDetailsUrl(requestDto.getPanDetailsUrl());
        submission.setPartnershipOrCoiUrl(requestDto.getPartnershipOrCoiUrl());
        submission.setDeedOrMsmeUrl(requestDto.getDeedOrMsmeUrl());
        submission.setBalanceSheetUrl(requestDto.getBalanceSheetUrl());

        submission.setRemarks(requestDto.getRemarks());
        submission.setStatus(VendorAccountsSubmissionStatus.PENDING);
        submission.setSentToAccountsBy(requestDto.getSentToAccountsBy());
        submission.setCreatedBy(requestDto.getSentToAccountsBy());
        submission.setUpdatedBy(requestDto.getSentToAccountsBy());

        VendorAccountsSubmission saved =
                vendorAccountsSubmissionRepository.save(submission);

        finalization.setSentToAccounts(true);
        finalization.setSentToAccountsBy(requestDto.getSentToAccountsBy());
        finalization.setSentToAccountsDate(new Date());
        finalization.setUpdatedBy(requestDto.getSentToAccountsBy());

        vendorFinalizationRepository.save(finalization);

        return mapAccountsSubmissionToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorAccountsSubmissionResponseDto> getAllSentToAccounts() {
        return vendorAccountsSubmissionRepository
                .findByIsDeletedFalseOrderBySentToAccountsDateDesc()
                .stream()
                .map(this::mapAccountsSubmissionToResponse)
                .toList();
    }

    @Override
    @Transactional
    public VendorAccountsSubmissionResponseDto approveByAccounts(
            Long submissionId,
            AccountsVendorFinalizationRequestDto requestDto
    ) {
        VendorAccountsSubmission submission = vendorAccountsSubmissionRepository
                .findByIdAndIsDeletedFalse(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor accounts submission not found",
                        "ERR_VENDOR_ACCOUNTS_SUBMISSION_NOT_FOUND"
                ));

        if (submission.getStatus() == VendorAccountsSubmissionStatus.APPROVED) {
            throw new ValidationException(
                    "Vendor accounts submission already approved",
                    "ERR_VENDOR_ACCOUNTS_ALREADY_APPROVED"
            );
        }

        if (submission.getStatus() == VendorAccountsSubmissionStatus.REJECTED) {
            throw new ValidationException(
                    "Rejected vendor accounts submission cannot be approved directly",
                    "ERR_VENDOR_ACCOUNTS_ALREADY_REJECTED"
            );
        }

        submission.setStatus(VendorAccountsSubmissionStatus.APPROVED);
        submission.setAccountsRemark(requestDto.getAccountsRemark());
        submission.setAccountsVerifiedBy(requestDto.getUserId());
        submission.setAccountsVerifiedDate(new Date());
        submission.setUpdatedBy(requestDto.getUserId());

        Vendor vendor = submission.getVendor();

        if (vendor == null) {
            throw new ResourceNotFoundException(
                    "Vendor not found for accounts submission",
                    "ERR_VENDOR_NOT_FOUND"
            );
        }

        vendor.setStatus(VendorStatus.ACTIVE);
        vendor.setUpdatedBy(requestDto.getUserId());
        vendor.setUpdatedDate(new Date());

        vendorRepository.save(vendor);

        VendorAccountsSubmission saved =
                vendorAccountsSubmissionRepository.save(submission);

        return mapAccountsSubmissionToResponse(saved);
    }

    @Override
    @Transactional
    public VendorAccountsSubmissionResponseDto rejectByAccounts(
            Long submissionId,
            AccountsVendorFinalizationRequestDto requestDto
    ) {
        VendorAccountsSubmission submission = vendorAccountsSubmissionRepository
                .findByIdAndIsDeletedFalse(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor accounts submission not found",
                        "ERR_VENDOR_ACCOUNTS_SUBMISSION_NOT_FOUND"
                ));

        if (submission.getStatus() == VendorAccountsSubmissionStatus.APPROVED) {
            throw new ValidationException(
                    "Approved vendor accounts submission cannot be rejected",
                    "ERR_VENDOR_ACCOUNTS_ALREADY_APPROVED"
            );
        }

        if (submission.getStatus() == VendorAccountsSubmissionStatus.REJECTED) {
            throw new ValidationException(
                    "Vendor accounts submission already rejected",
                    "ERR_VENDOR_ACCOUNTS_ALREADY_REJECTED"
            );
        }

        submission.setStatus(VendorAccountsSubmissionStatus.REJECTED);
        submission.setAccountsRemark(requestDto.getAccountsRemark());
        submission.setAccountsVerifiedBy(requestDto.getUserId());
        submission.setAccountsVerifiedDate(new Date());
        submission.setUpdatedBy(requestDto.getUserId());

        VendorAccountsSubmission saved =
                vendorAccountsSubmissionRepository.save(submission);

        return mapAccountsSubmissionToResponse(saved);
    }

    private VendorAccountsSubmissionResponseDto mapAccountsSubmissionToResponse(
            VendorAccountsSubmission submission
    ) {
        VendorAccountsSubmissionResponseDto response =
                new VendorAccountsSubmissionResponseDto();

        response.setId(submission.getId());

        if (submission.getVendorFinalization() != null) {
            response.setVendorFinalizationId(
                    submission.getVendorFinalization().getId()
            );
        }

        if (submission.getVendor() != null) {
            response.setVendorId(submission.getVendor().getId());
            response.setVendorName(submission.getVendor().getName());
            response.setVendorEmail(submission.getVendor().getEmail());
            response.setVendorMobile(submission.getVendor().getMobile());
        }

        if (submission.getRfq() != null) {
            response.setRfqId(submission.getRfq().getId());
            response.setRfqNumber(submission.getRfq().getRfqNumber());
        }

        if (submission.getQuotation() != null) {
            response.setQuotationId(submission.getQuotation().getId());
            response.setQuotationNumber(submission.getQuotation().getQuotationNumber());
        }

        response.setName(submission.getName());
        response.setNumber(submission.getNumber());
        response.setEmail(submission.getEmail());
        response.setAadhar(submission.getAadhar());

        response.setAuthorizedSignatoryName(submission.getAuthorizedSignatoryName());
        response.setAuthorizedSignatoryNumber(submission.getAuthorizedSignatoryNumber());
        response.setAuthorizedSignatoryEmail(submission.getAuthorizedSignatoryEmail());
        response.setAuthorizedSignatoryAadhar(submission.getAuthorizedSignatoryAadhar());

        response.setAccountHolderName(submission.getAccountHolderName());
        response.setAccountNumber(submission.getAccountNumber());
        response.setIfsc(submission.getIfsc());
        response.setSwiftCode(submission.getSwiftCode());
        response.setBranchAddress(submission.getBranchAddress());

        response.setGstDetailsUrl(submission.getGstDetailsUrl());
        response.setVendorSetupFormUrl(submission.getVendorSetupFormUrl());
        response.setCancelChequeUrl(submission.getCancelChequeUrl());
        response.setItrLastFinancialYearUrl(submission.getItrLastFinancialYearUrl());
        response.setPanDetailsUrl(submission.getPanDetailsUrl());
        response.setPartnershipOrCoiUrl(submission.getPartnershipOrCoiUrl());
        response.setDeedOrMsmeUrl(submission.getDeedOrMsmeUrl());
        response.setBalanceSheetUrl(submission.getBalanceSheetUrl());

        response.setRemarks(submission.getRemarks());
        response.setStatus(
                submission.getStatus() != null ? submission.getStatus().name() : null
        );

        response.setSentToAccountsBy(submission.getSentToAccountsBy());
        response.setSentToAccountsDate(submission.getSentToAccountsDate());

        response.setAccountsVerifiedBy(submission.getAccountsVerifiedBy());
        response.setAccountsVerifiedDate(submission.getAccountsVerifiedDate());
        response.setAccountsRemark(submission.getAccountsRemark());

        response.setCreatedBy(submission.getCreatedBy());
        response.setUpdatedBy(submission.getUpdatedBy());
        response.setCreatedDate(submission.getCreatedDate());
        response.setUpdatedDate(submission.getUpdatedDate());
        response.setDeleted(submission.isDeleted());

        return response;
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

        response.setSentToAccounts(finalization.isSentToAccounts());
        response.setSentToAccountsBy(finalization.getSentToAccountsBy());
        response.setSentToAccountsDate(finalization.getSentToAccountsDate());

        return response;
    }



}