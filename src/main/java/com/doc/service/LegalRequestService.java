package com.doc.service;

import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.em.LegalStatus;
import com.doc.entity.LegalRequest.LegalRequest;
import com.doc.entity.document.LegalRequestDocument;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LegalRequestService {

    LegalRequestDto createRequest(Long projectId,
                               Long milestoneId,
                               double tatInDays,
                               MultipartFile[] files) throws IOException;

    LegalRequestDto mapToResponse(LegalRequest request);

    LegalRequestDto updateStatus(Long id, LegalStatus status, String reason);
     Page<LegalRequestDto> getLegalRequests(Long userId, String role, int page, int size);
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

}

