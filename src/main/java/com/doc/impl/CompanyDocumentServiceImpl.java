package com.doc.impl;


import com.doc.dto.document.*;
import com.doc.entity.client.Company;
import com.doc.entity.document.*;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.*;
import com.doc.repository.documentRepo.*;
import com.doc.service.CompanyDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompanyDocumentServiceImpl implements CompanyDocumentService {

    @Autowired private CompanyRepository companyRepo;
    @Autowired private ProductRequiredDocumentsRepository reqDocRepo;
    @Autowired private CompanyDocumentRepository companyDocumentRepository;
    @Autowired private UserRepository userRepo;
    @Autowired private DocumentStatusRepository statusRepo;

    @Value("${aws.s3.bucket.url}")
    private String bucketUrl;

    @Override
    public CompanyDocumentResponseDto uploadCompanyDocument(CompanyDocumentUploadRequestDto dto) {
        Company company = getCompany(dto.getCompanyId());
        ProductRequiredDocuments reqDoc = getRequiredDocument(dto.getRequiredDocumentId());
        User uploadedBy = getUser(dto.getUploadedById());
        User createdBy = getUser(dto.getCreatedById());

        validateUpload(dto, reqDoc);

        String cleanName = sanitizeFileName(dto.getFileName());
        String fileUrl = bucketUrl + "/" + cleanName;

        CompanyDocument existing = companyDocumentRepository
                .findByCompanyIdAndRequiredDocumentIdAndIsDeletedFalse(dto.getCompanyId(), dto.getRequiredDocumentId())
                .orElse(null);

        CompanyDocument doc = existing != null ? existing : new CompanyDocument();
        boolean isReplacement = existing != null;

        if (isReplacement && "VERIFIED".equals(doc.getStatus().getName())) {
            throw new ValidationException("Cannot replace VERIFIED company document", "VERIFIED_DOC_REPLACE");
        }

        updateDocFromDto(doc, dto, company, reqDoc, uploadedBy, createdBy, fileUrl, cleanName, isReplacement);
        doc = companyDocumentRepository.save(doc);

        return mapToResponseDto(doc);
    }

    @Override
    public CompanyDocumentResponseDto updateStatus(Long docId, CompanyDocumentStatusUpdateDto dto) {
        CompanyDocument doc = getCompanyDoc(docId);
        DocumentStatus newStatus = statusRepo.findByName(dto.getNewStatus())
                .orElseThrow(() -> new ResourceNotFoundException("Status not found", "STATUS_NOT_FOUND"));

        if ("REJECTED".equals(dto.getNewStatus()) && (dto.getRemarks() == null || dto.getRemarks().isBlank())) {
            throw new ValidationException("Remarks required for REJECTED", "MISSING_REMARKS");
        }

        User changedBy = getUser(dto.getChangedById());

        doc.setStatus(newStatus);
        doc.setRemarks(dto.getRemarks());
        doc.setUpdatedBy(changedBy.getId());
        doc.setUpdatedDate(new Date());

        if ("VERIFIED".equals(dto.getNewStatus())) {
            doc.setVerifiedBy(changedBy);
            doc.setVerifiedDate(new Date());
            doc.setValidationPassed(true);
            doc.setQualityScore(calculateQualityScore(doc));
        }

        doc = companyDocumentRepository.save(doc);
        return mapToResponseDto(doc);
    }

    @Override
    public List<CompanyDocumentResponseDto> getVerifiedDocuments(Long companyId) {
        return companyDocumentRepository.findByCompanyIdAndStatusNameAndIsDeletedFalse(companyId, "VERIFIED")
                .stream()
                .filter(CompanyDocument::isReusable)
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompanyDocCheckResponseDto checkAvailability(Long companyId, Long requiredDocumentId) {
        Optional<CompanyDocument> docOpt = companyDocumentRepository
                .findByCompanyIdAndRequiredDocumentIdAndStatusNameAndIsDeletedFalse(companyId, requiredDocumentId, "VERIFIED");

        CompanyDocCheckResponseDto resp = new CompanyDocCheckResponseDto();
        if (docOpt.isPresent() && docOpt.get().isReusable()) {
            CompanyDocument doc = docOpt.get();
            resp.setAvailable(true);
            resp.setFileUrl(doc.getFileUrl());
            resp.setFileName(doc.getFileName());
            resp.setExpiryDate(toLocalDate(doc.getExpiryDate()));
            resp.setDaysUntilExpiry(doc.getDaysUntilExpiry());
            resp.setPermanent(doc.isPermanent());
            resp.setVerifiedDate(toLocalDate(doc.getVerifiedDate()));
            resp.setValidationPassed(doc.isValidationPassed());
            resp.setExpiryStatus(getExpiryStatus(doc));
            resp.setMessage("Auto-filled from company library");
        } else {
            resp.setAvailable(false);
            resp.setMessage("Please upload");
        }
        return resp;
    }

    private void validateUpload(CompanyDocumentUploadRequestDto dto, ProductRequiredDocuments reqDoc) {
        if (reqDoc.isExpiring() && dto.getExpiryDate() == null && (dto.getIsPermanent() == null || !dto.getIsPermanent())) {
            throw new ValidationException("Expiry date required for EXPIRING document", "EXPIRY_REQUIRED");
        }
        if (reqDoc.isFixed() && dto.getExpiryDate() != null) {
            throw new ValidationException("Expiry not allowed for FIXED document", "EXPIRY_NOT_ALLOWED");
        }
    }

    private void updateDocFromDto(CompanyDocument doc, CompanyDocumentUploadRequestDto dto,
                                  Company company, ProductRequiredDocuments reqDoc,
                                  User uploadedBy, User createdBy, String fileUrl,
                                  String fileName, boolean isReplacement) {
        doc.setCompany(company);
        doc.setRequiredDocument(reqDoc);
        doc.setFileUrl(fileUrl);
        doc.setFileName(fileName);
        doc.setFileSizeKb(dto.getFileSizeKb());
        doc.setFileFormat(dto.getFileFormat());
        doc.setUploadedBy(uploadedBy);
        doc.setUploadTime(new Date());
        doc.setStatus(statusRepo.findByName("UPLOADED").orElseThrow());
        doc.setValidationPassed(false);
        doc.setQualityScore(0.0);

        if (isReplacement) {
            doc.setOldFileUrl(doc.getFileUrl());
            doc.setOldFileName(doc.getFileName());
            doc.setReplacementCount(doc.getReplacementCount() + 1);
        } else {
            doc.setCreatedBy(dto.getCreatedById());
            doc.setCreatedDate(new Date());
            doc.setReplacementCount(0);
        }

        doc.setUpdatedBy(dto.getCreatedById());
        doc.setUpdatedDate(new Date());

        // Expiry logic
        if (reqDoc.isFixed() || (dto.getIsPermanent() != null && dto.getIsPermanent())) {
            doc.setPermanent(true);
            doc.setExpiryDate(null);
        } else if (reqDoc.isExpiring() && dto.getExpiryDate() != null) {
            doc.setExpiryDate(Date.from(dto.getExpiryDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            doc.setExpirySetBy(dto.getUploadedById());
            doc.setExpirySetDate(new Date());
            doc.setLastRenewalDate(doc.getExpiryDate());
        }
    }

    private String getExpiryStatus(CompanyDocument doc) {
        if (doc.isPermanent()) return "PERMANENT";
        if (doc.isExpired()) return "EXPIRED";
        return "VALID";
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Double calculateQualityScore(CompanyDocument doc) {
        return 0.95; // Mock AI OCR score
    }

    private Company getCompany(Long id) {
        return companyRepo.findActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found", "COMPANY_NOT_FOUND"));
    }

    private ProductRequiredDocuments getRequiredDocument(Long id) {
        return reqDocRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Required document not found", "DOC_NOT_FOUND"));
    }

    private User getUser(Long id) {
        return userRepo.findActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));
    }

    private CompanyDocument getCompanyDoc(Long id) {
        return companyDocumentRepository.findActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company document not found", "DOC_NOT_FOUND"));
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.\\-_]", "");
    }

    private CompanyDocumentResponseDto mapToResponseDto(CompanyDocument doc) {
        CompanyDocumentResponseDto dto = new CompanyDocumentResponseDto();
        dto.setId(doc.getId());
        dto.setRequiredDocumentId(doc.getRequiredDocument().getId());
        dto.setDocumentName(doc.getRequiredDocument().getName());
        dto.setDocumentType(doc.getRequiredDocument().getType());
        dto.setFileUrl(doc.getFileUrl());
        dto.setFileName(doc.getFileName());
        dto.setStatus(doc.getStatus().getName());
        dto.setExpiryDate(toLocalDate(doc.getExpiryDate()));
        dto.setDaysUntilExpiry(doc.getDaysUntilExpiry());
        dto.setExpiryStatus(getExpiryStatus(doc));
        dto.setReusable(doc.isReusable());
        dto.setVerifiedDate(toLocalDate(doc.getVerifiedDate()));
        if (doc.getVerifiedBy() != null) {
            dto.setVerifiedById(doc.getVerifiedBy().getId());
            dto.setVerifiedByName(doc.getVerifiedBy().getFullName());
        }
        dto.setFileSizeKb(doc.getFileSizeKb());
        dto.setFileFormat(doc.getFileFormat());
        dto.setValidationPassed(doc.isValidationPassed());
        dto.setValidationIssues(doc.getValidationIssues());
        dto.setQualityScore(doc.getQualityScore());
        dto.setReplacementCount(doc.getReplacementCount());
        return dto;
    }
}