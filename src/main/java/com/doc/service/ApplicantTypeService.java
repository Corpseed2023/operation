package com.doc.service;

import com.doc.dto.document.ApplicantTypeRequestDto;
import com.doc.dto.document.ApplicantTypeResponseDto;

import java.util.List;

public interface ApplicantTypeService {

    ApplicantTypeResponseDto createApplicantType(ApplicantTypeRequestDto dto);

    ApplicantTypeResponseDto updateApplicantType(Long id, ApplicantTypeRequestDto dto);

    ApplicantTypeResponseDto getApplicantTypeById(Long id);

    List<ApplicantTypeResponseDto> getApplicantTypesPaginated(int page, int size);

       void softDeleteApplicantType(Long id);
}