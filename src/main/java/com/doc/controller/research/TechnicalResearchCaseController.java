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

    @GetMapping
    public ResponseEntity<Page<TechnicalResearchCaseResponseDto>> getCases(
            @RequestParam
            @Positive(message = "User ID must be greater than zero")
            Long userId,

            @RequestParam(required = false)
            TechnicalResearchCaseStatus status,

            @RequestParam(required = false)
            ResearchPriority priority,

            @RequestParam(required = false)
            @Positive(message = "Product ID must be greater than zero")
            Long productId,

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
                        userId,
                        status,
                        priority,
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


    /**
     * Returns all research cases visible to the user.
     *
     * A case is returned when the user:
     * 1. Raised it as a salesperson, or
     * 2. Is currently assigned to it as a technical person.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<TechnicalResearchCaseResponseDto>>
    getCasesForUser(
            @PathVariable
            @Positive(message = "User ID must be greater than zero")
            Long userId,

            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "createdAt",
                    direction = DESC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                researchCaseService.getCasesForUser(
                        userId,
                        pageable
                )
        );
    }

    @PostMapping("/{caseId}/assignments")
    public ResponseEntity<TechnicalResearchCaseResponseDto> assignCase(
            @PathVariable
            @Positive(message = "Case ID must be greater than zero")
            Long caseId,

            @RequestParam
            @Positive(message = "Assignee user ID must be greater than zero")
            Long assigneeUserId,

            @RequestParam
            @Positive(message = "Assigned-by user ID must be greater than zero")
            Long assignedByUserId
    ) {
        return ResponseEntity.ok(
                researchCaseService.assignCase(
                        caseId,
                        assigneeUserId,
                        assignedByUserId
                )
        );
    }

    @GetMapping("/lead/{leadId}")
    public ResponseEntity<Page<TechnicalResearchCaseResponseDto>>
    getCasesByLeadId(
            @PathVariable
            @Positive(message = "Lead ID must be greater than zero")
            Long leadId,

            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "createdAt",
                    direction = DESC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                researchCaseService.getCasesByLeadId(
                        leadId,
                        pageable
                )
        );
    }



}