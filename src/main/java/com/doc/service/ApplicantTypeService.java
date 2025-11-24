// src/main/java/com/doc/service/ApplicantTypeService.java
package com.doc.service;

import com.doc.dto.document.ApplicantTypeRequestDto;
import com.doc.dto.document.ApplicantTypeResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ApplicantTypeService {
    ApplicantTypeResponseDto createApplicantType(ApplicantTypeRequestDto dto);
    ApplicantTypeResponseDto updateApplicantType(Long id, ApplicantTypeRequestDto dto);
    void deleteApplicantType(Long id); // soft delete
    ApplicantTypeResponseDto getApplicantTypeById(Long id);
    List<ApplicantTypeResponseDto> getAllActiveApplicantTypes();
    Page<ApplicantTypeResponseDto> getApplicantTypesPaged(int page, int size);
}