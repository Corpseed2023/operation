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
            Long userId,
            TechnicalResearchCaseStatus status,
            ResearchPriority priority,
            String search,
            Pageable pageable
    );

    long getActiveAssignmentCount(Long assigneeUserId);

    Page<TechnicalResearchCaseResponseDto> getCasesForUser(
            Long userId,
            Pageable pageable
    );


    TechnicalResearchCaseResponseDto assignCase(
            Long caseId,
            Long assigneeUserId,
            Long assignedByUserId
    );

}