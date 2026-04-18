package com.doc.controller.legalrequest;

import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.dto.LegalRequestDto.LegalStatusUpdateDto;
import com.doc.em.LegalStatus;
import com.doc.service.LegalRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/legal-requests")
public class LegalRequestController {

    private final LegalRequestService legalRequestService;

    @Autowired
    public LegalRequestController(LegalRequestService legalRequestService) {
        this.legalRequestService = legalRequestService;
    }

    /**
     * Create a new Legal Request
     */
    @PostMapping
    public ResponseEntity<LegalRequestDto> createRequest(@RequestBody LegalRequestDto dto) {
        LegalRequestDto response = legalRequestService.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update Legal Request Status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<LegalRequestDto> updateStatus(
            @PathVariable Long id,
            @RequestBody LegalStatusUpdateDto dto) {

        LegalRequestDto response = legalRequestService.updateStatus(id, dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Get Legal Request by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LegalRequestDto> getLegalRequestById(@PathVariable Long id) {
        LegalRequestDto response = legalRequestService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Search/Filter Legal Requests with pagination
     */
    @GetMapping
    public ResponseEntity<Page<LegalRequestDto>> searchRequests(
            @RequestParam(required = false) LegalStatus status,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String milestoneName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<LegalRequestDto> result = legalRequestService.searchRequests(
                status, projectId, assignedTo, createdBy,
                projectName, milestoneName, startDate, endDate, page, size
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Mark Legal Request as Viewed
     */
    @PatchMapping("/{id}/view")
    public ResponseEntity<LegalRequestDto> markAsViewed(
            @PathVariable Long id,
            @RequestParam Long userId) {

        LegalRequestDto response = legalRequestService.markAsViewed(id, userId);
        return ResponseEntity.ok(response);
    }

}