package com.doc.controller.legalrequest;

import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.dto.LegalRequestDto.LegalRequestResponseDto;
import com.doc.dto.LegalRequestDto.LegalStatusUpdateDto;
import com.doc.em.LegalStatus;
import com.doc.service.LegalRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/legal-requests")
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
    public ResponseEntity<LegalRequestResponseDto> createRequest(
            @RequestBody LegalRequestDto dto) {

        LegalRequestResponseDto response = legalRequestService.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update Legal Request Status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<LegalRequestResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody LegalStatusUpdateDto dto) {

        LegalRequestResponseDto response = legalRequestService.updateStatus(id, dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Get Legal Request by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LegalRequestResponseDto> getLegalRequestById(
            @PathVariable Long id) {

        LegalRequestResponseDto response = legalRequestService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark Legal Request as Viewed
     */
    @PatchMapping("/{id}/view")
    public ResponseEntity<LegalRequestResponseDto> markAsViewed(
            @PathVariable Long id,
            @RequestParam Long userId) {

        LegalRequestResponseDto response = legalRequestService.markAsViewed(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get All Legal Requests
     */
    @GetMapping
    public ResponseEntity<Page<LegalRequestResponseDto>> getAllLegalRequests(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "INITIATED") LegalStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<LegalRequestResponseDto> response =
                legalRequestService.getAllLegalRequests(userId, status, page, size);

        return ResponseEntity.ok(response);
    }
}