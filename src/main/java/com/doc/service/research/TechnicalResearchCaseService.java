package com.doc.service.research;

import com.doc.dto.research.*;
import com.doc.entity.research.ResearchPriority;
import com.doc.entity.research.TechnicalResearchCaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TechnicalResearchCaseService {

    TechnicalResearchCaseResponseDto createCase(
            TechnicalResearchCaseCreateRequestDto request
    );



    TechnicalResearchCaseResponseDto getCaseById(Long caseId);

    Page<TechnicalResearchCaseResponseDto> getCases(
            TechnicalResearchCaseStatus status,
            ResearchPriority priority,
            Long productId,
            Long raisedByUserId,
            Long assigneeUserId,
            String search,
            Pageable pageable
    );

    long getActiveAssignmentCount(Long assigneeUserId);

    Page<TechnicalResearchCaseResponseDto> getCasesForUser(
            Long userId,
            Pageable pageable
    );


}