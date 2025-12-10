// src/main/java/com/doc/service/ApplicantTypeService.java
package com.doc.service;

import com.doc.dto.document.ApplicantTypeRequestDto;
import com.doc.dto.document.ApplicantTypeResponseDto;

import java.util.List;

public interface ApplicantTypeService {
    ApplicantTypeResponseDto createApplicantType(ApplicantTypeRequestDto dto);
    ApplicantTypeResponseDto updateApplicantType(Long id, ApplicantTypeRequestDto dto);
    ApplicantTypeResponseDto getApplicantTypeById(Long id);

    // Custom pagination: page starts from 1
    List<ApplicantTypeResponseDto> getApplicantTypesPaginated(int page, int size);
}