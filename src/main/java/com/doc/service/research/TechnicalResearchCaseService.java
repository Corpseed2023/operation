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

    TechnicalResearchCaseResponseDto assignCase(
            Long caseId,
            TechnicalResearchAssignmentRequestDto request
    );

    TechnicalResearchCaseResponseDto startWork(
            Long caseId,
            TechnicalResearchActionRequestDto request
    );

    TechnicalResearchCaseResponseDto submitCase(
            Long caseId,
            TechnicalResearchSubmissionRequestDto request
    );

    TechnicalResearchCaseResponseDto requestRevision(
            Long caseId,
            TechnicalResearchClosureRequestDto request
    );

    TechnicalResearchCaseResponseDto completeCase(
            Long caseId,
            TechnicalResearchActionRequestDto request
    );

    TechnicalResearchCaseResponseDto rejectCase(
            Long caseId,
            TechnicalResearchClosureRequestDto request
    );

    TechnicalResearchCaseResponseDto cancelCase(
            Long caseId,
            TechnicalResearchClosureRequestDto request
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
}