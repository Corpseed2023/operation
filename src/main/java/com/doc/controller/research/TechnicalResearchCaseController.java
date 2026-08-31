package com.doc.controller.research;

import com.doc.dto.research.*;
import com.doc.entity.research.ResearchPriority;
import com.doc.entity.research.TechnicalResearchCaseStatus;
import com.doc.service.research.TechnicalResearchCaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/operationService/api/technical-research-cases")
@RequiredArgsConstructor
@Validated
public class TechnicalResearchCaseController {

    private final TechnicalResearchCaseService researchCaseService;

    /**
     * Salesperson creates a technical research case directly
     * in Operation Service.
     */
    @PostMapping
    public ResponseEntity<TechnicalResearchCaseResponseDto> createCase(
            @Valid
            @RequestBody
            TechnicalResearchCaseCreateRequestDto request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(researchCaseService.createCase(request));
    }

    /**
     * Manager assigns or reassigns a case.
     */
    @PostMapping("/{caseId}/assignments")
    public ResponseEntity<TechnicalResearchCaseResponseDto> assignCase(
            @PathVariable
            @Positive(message = "Case ID must be greater than zero")
            Long caseId,

            @Valid
            @RequestBody
            TechnicalResearchAssignmentRequestDto request
    ) {
        return ResponseEntity.ok(
                researchCaseService.assignCase(caseId, request)
        );
    }

    /**
     * Assigned technical person starts the research.
     */
    @PatchMapping("/{caseId}/start")
    public ResponseEntity<TechnicalResearchCaseResponseDto> startWork(
            @PathVariable
            @Positive(message = "Case ID must be greater than zero")
            Long caseId,

            @Valid
            @RequestBody
            TechnicalResearchActionRequestDto request
    ) {
        return ResponseEntity.ok(
                researchCaseService.startWork(caseId, request)
        );
    }

    /**
     * Assigned technical person submits findings and recommendation.
     */
    @PatchMapping("/{caseId}/submit")
    public ResponseEntity<TechnicalResearchCaseResponseDto> submitCase(
            @PathVariable
            @Positive(message = "Case ID must be greater than zero")
            Long caseId,

            @Valid
            @RequestBody
            TechnicalResearchSubmissionRequestDto request
    ) {
        return ResponseEntity.ok(
                researchCaseService.submitCase(caseId, request)
        );
    }

    /**atua
     * Manager returns submitted research for revision.
     */
    @PatchMapping("/{caseId}/request-revision")
    public ResponseEntity<TechnicalResearchCaseResponseDto> requestRevision(
            @PathVariable
            @Positive(message = "Case ID must be greater than zero")
            Long caseId,

            @Valid
            @RequestBody
            TechnicalResearchClosureRequestDto request
    ) {
        return ResponseEntity.ok(
                researchCaseService.requestRevision(caseId, request)
        );
    }

    /**
     * Manager approves and completes submitted research.
     */
    @PatchMapping("/{caseId}/complete")
    public ResponseEntity<TechnicalResearchCaseResponseDto> completeCase(
            @PathVariable
            @Positive(message = "Case ID must be greater than zero")
            Long caseId,

            @Valid
            @RequestBody
            TechnicalResearchActionRequestDto request
    ) {
        return ResponseEntity.ok(
                researchCaseService.completeCase(caseId, request)
        );
    }

    /**
     * Manager rejects the research case.
     */
    @PatchMapping("/{caseId}/reject")
    public ResponseEntity<TechnicalResearchCaseResponseDto> rejectCase(
            @PathVariable
            @Positive(message = "Case ID must be greater than zero")
            Long caseId,

            @Valid
            @RequestBody
            TechnicalResearchClosureRequestDto request
    ) {
        return ResponseEntity.ok(
                researchCaseService.rejectCase(caseId, request)
        );
    }

    /**
     * Salesperson or manager cancels the case.
     */
    @PatchMapping("/{caseId}/cancel")
    public ResponseEntity<TechnicalResearchCaseResponseDto> cancelCase(
            @PathVariable
            @Positive(message = "Case ID must be greater than zero")
            Long caseId,

            @Valid
            @RequestBody
            TechnicalResearchClosureRequestDto request
    ) {
        return ResponseEntity.ok(
                researchCaseService.cancelCase(caseId, request)
        );
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<TechnicalResearchCaseResponseDto> getCaseById(
            @PathVariable
            @Positive(message = "Case ID must be greater than zero")
            Long caseId
    ) {
        return ResponseEntity.ok(
                researchCaseService.getCaseById(caseId)
        );
    }

    /**
     * Search and filter research cases.
     *
     * Examples:
     * GET /technical-research-cases?status=IN_PROGRESS
     * GET /technical-research-cases?assigneeUserId=15
     * GET /technical-research-cases?productId=8
     */
    @GetMapping
    public ResponseEntity<Page<TechnicalResearchCaseResponseDto>> getCases(
            @RequestParam(required = false)
            TechnicalResearchCaseStatus status,

            @RequestParam(required = false)
            ResearchPriority priority,

            @RequestParam(required = false)
            @Positive(message = "Product ID must be greater than zero")
            Long productId,

            @RequestParam(required = false)
            @Positive(message = "Raised-by user ID must be greater than zero")
            Long raisedByUserId,

            @RequestParam(required = false)
            @Positive(message = "Assignee user ID must be greater than zero")
            Long assigneeUserId,

            @RequestParam(required = false)
            String search,

            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "createdAt",
                    direction = DESC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                researchCaseService.getCases(
                        status,
                        priority,
                        productId,
                        raisedByUserId,
                        assigneeUserId,
                        search,
                        pageable
                )
        );
    }

    /**
     * Returns current active workload of a technical user.
     */
    @GetMapping("/assignees/{assigneeUserId}/active-count")
    public ResponseEntity<Long> getActiveAssignmentCount(
            @PathVariable
            @Positive(message = "Assignee user ID must be greater than zero")
            Long assigneeUserId
    ) {
        return ResponseEntity.ok(
                researchCaseService.getActiveAssignmentCount(
                        assigneeUserId
                )
        );
    }
}