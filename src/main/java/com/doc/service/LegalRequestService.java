package com.doc.service;

import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.dto.LegalRequestDto.LegalStatusUpdateDto;
import com.doc.em.LegalStatus;
import com.doc.entity.legalrequest.LegalRequest;

import org.springframework.data.domain.Page;

import java.time.LocalDateTime;


public interface LegalRequestService {

    LegalRequestDto createRequest(LegalRequestDto dto);

    LegalRequestDto mapToResponse(LegalRequest request);

    LegalRequestDto updateStatus(Long id, LegalStatusUpdateDto dto);

     LegalRequestDto getById(Long id);

    LegalRequestDto markAsViewed(Long id, Long userId);

}

