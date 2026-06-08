package com.doc.service;

import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.dto.LegalRequestDto.LegalRequestResponseDto;
import com.doc.dto.LegalRequestDto.LegalStatusUpdateDto;
import com.doc.em.LegalStatus;
import org.springframework.data.domain.Page;

public interface LegalRequestService {

    LegalRequestResponseDto createRequest(LegalRequestDto dto);

    LegalRequestResponseDto updateStatus(Long id, LegalStatusUpdateDto dto);

    LegalRequestResponseDto getById(Long id);

    LegalRequestResponseDto markAsViewed(Long id, Long userId);

    Page<LegalRequestResponseDto> getAllLegalRequests(
            Long userId,
            LegalStatus status,
            int page,
            int size
    );
}