package com.doc.service;

import com.doc.dto.company.MyCompanyDocumentRequestDto;
import com.doc.dto.company.MyCompanyDocumentResponseDto;
import com.doc.entity.document.MyCompanyDocument;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.MyCompanyDocumentRepository;
import com.doc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyCompanyDocumentServiceImpl implements MyCompanyDocumentService {

    private final MyCompanyDocumentRepository myCompanyDocumentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MyCompanyDocumentResponseDto upload(MyCompanyDocumentRequestDto request, Long currentUserId) {
        User uploadedBy = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));

        MyCompanyDocument entity = new MyCompanyDocument();
        applyRequest(entity, request);
        entity.setUploadedBy(uploadedBy);
        entity.setUploadTime(new Date());

        MyCompanyDocument saved = myCompanyDocumentRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public MyCompanyDocumentResponseDto update(Long id, MyCompanyDocumentRequestDto request, Long currentUserId) {
        MyCompanyDocument entity = myCompanyDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found with id: " + id, "DOCUMENT_NOT_FOUND"));

        User uploadedBy = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));

        applyRequest(entity, request);
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
    public MyCompanyDocumentResponseDto getById(Long id) {
        MyCompanyDocument entity = myCompanyDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found with id: " + id, "DOCUMENT_NOT_FOUND"));
        return toDto(entity);
    }

    @Override
    public List<MyCompanyDocumentResponseDto> getByType(String documentType) {
        return myCompanyDocumentRepository.findByDocumentTypeIgnoreCase(documentType).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!myCompanyDocumentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Document not found with id: " + id, "DOCUMENT_NOT_FOUND");
        }
        myCompanyDocumentRepository.deleteById(id);
    }

    private void applyRequest(MyCompanyDocument entity, MyCompanyDocumentRequestDto request) {
        entity.setDocumentType(request.getDocumentType());
        entity.setFileUrl(request.getFileUrl());
        entity.setFileName(request.getFileName());
        entity.setFileSizeKb(request.getFileSizeKb());
        entity.setFileFormat(request.getFileFormat());
        entity.setDocumentNumber(request.getDocumentNumber());
        entity.setRemarks(request.getRemarks());
    }

    private MyCompanyDocumentResponseDto toDto(MyCompanyDocument entity) {
        return MyCompanyDocumentResponseDto.builder()
                .id(entity.getId())
                .documentType(entity.getDocumentType())
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