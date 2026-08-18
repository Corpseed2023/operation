package com.doc.service;

import com.doc.dto.company.MyCompanyDocumentRequestDto;
import com.doc.dto.company.MyCompanyDocumentResponseDto;
import com.doc.entity.document.MyCompanyDocument;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.MyCompanyDocumentRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.documentRepo.ProductRequiredDocumentsRepository;
import com.doc.service.MyCompanyDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyCompanyDocumentServiceImpl implements MyCompanyDocumentService {

    private final MyCompanyDocumentRepository myCompanyDocumentRepository;
    private final ProductRequiredDocumentsRepository requiredDocumentsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MyCompanyDocumentResponseDto uploadOrReplace(MyCompanyDocumentRequestDto request, Long currentUserId) {

        ProductRequiredDocuments requiredDocument = requiredDocumentsRepository
                .findById(request.getRequiredDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Required document not found with id: " + request.getRequiredDocumentId(),"DOCUMENT_NOT_FOUND"));

        User uploadedBy = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found  " + "USER_NOT_FOUND","USER_NOT_FOUND"));

        // Check if this document type already exists — if so, update it in place.
        MyCompanyDocument entity = myCompanyDocumentRepository
                .findByRequiredDocument_Id(request.getRequiredDocumentId())
                .orElseGet(MyCompanyDocument::new);

        entity.setRequiredDocument(requiredDocument);
        entity.setFileUrl(request.getFileUrl());
        entity.setFileName(request.getFileName());
        entity.setFileSizeKb(request.getFileSizeKb());
        entity.setFileFormat(request.getFileFormat());
        entity.setDocumentNumber(request.getDocumentNumber());
        entity.setRemarks(request.getRemarks());
        entity.setUploadedBy(uploadedBy);
        entity.setUploadTime(new Date());

        MyCompanyDocument saved = myCompanyDocumentRepository.save(entity);
        return toDto(saved);
    }

    @Override
    public List<MyCompanyDocumentResponseDto> getAll() {
        return myCompanyDocumentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public MyCompanyDocumentResponseDto getByRequiredDocumentId(Long requiredDocumentId) {
        MyCompanyDocument entity = myCompanyDocumentRepository
                .findByRequiredDocument_Id(requiredDocumentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No document uploaded yet for required document id: " + requiredDocumentId,"DOCUMENT_NOT_FOUND"));

        return toDto(entity);
    }

    private MyCompanyDocumentResponseDto toDto(MyCompanyDocument entity) {
        return MyCompanyDocumentResponseDto.builder()
                .id(entity.getId())
                .requiredDocumentId(entity.getRequiredDocument().getId())
                .requiredDocumentName(entity.getRequiredDocument().getName())
                .requiredDocumentType(entity.getRequiredDocument().getType())
                .fileUrl(entity.getFileUrl())
                .fileName(entity.getFileName())
                .fileSizeKb(entity.getFileSizeKb())
                .fileFormat(entity.getFileFormat())
                .documentNumber(entity.getDocumentNumber())
                .remarks(entity.getRemarks())
                .uploadedById(entity.getUploadedBy().getId())
                .uploadedByName(entity.getUploadedBy().getFullName())
                .uploadTime(entity.getUploadTime())
                .createdDate(entity.getCreatedDate())
                .updatedDate(entity.getUpdatedDate())
                .build();
    }
}