package com.doc.service;

import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.dto.LegalRequestDto.LegalStatusUpdateDto;
import com.doc.em.LegalStatus;
import com.doc.entity.LegalRequest.LegalRequest;

import com.doc.entity.user.User;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;


public interface LegalRequestService {

    LegalRequestDto createRequest(LegalRequestDto dto);



    LegalRequestDto mapToResponse(LegalRequest request);

    LegalRequestDto updateStatus(Long id, LegalStatusUpdateDto dto);


     Page<LegalRequestDto> getLegalRequests(Long userId, int page, int size);



     Page<LegalRequestDto> searchRequests(
            LegalStatus status,
            Long projectId,
            Long assignedTo,
            Long createdBy,
            String projectName,
            String milestoneName,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size
    );


     LegalRequestDto getById(Long id);
    LegalRequestDto assignRequest(Long requestId, Long assignedToLegal, String note);

    LegalRequestDto markAsViewed(Long id, Long userId);

    LegalRequestDto updateTat(Long id, LegalRequestDto dto);
}

