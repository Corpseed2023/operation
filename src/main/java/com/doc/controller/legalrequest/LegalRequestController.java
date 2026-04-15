package com.doc.controller.legalrequest;
import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.em.LegalStatus;
import com.doc.entity.LegalRequest.LegalRequest;
import com.doc.service.LegalRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/legal-request")
public class LegalRequestController {

    @Autowired
    private LegalRequestService legalRequestService;

    @PostMapping("/create")
    public LegalRequestDto createRequest(
            @RequestParam Long projectId,
            @RequestParam Long milestoneId,
            @RequestParam double tatInDays,
            @RequestParam(value = "files", required = false) MultipartFile[] files) throws Exception {

        return legalRequestService.createRequest(
                projectId,
                milestoneId,
                tatInDays,
                files
        );
    }
    @PatchMapping("/{id}/status")
    public LegalRequestDto updateStatus(
            @PathVariable Long id,
            @RequestParam LegalStatus status,
            @RequestParam(required = false) String reason
    ) {
        return legalRequestService.updateStatus(id, status, reason);
    }

    @GetMapping("/{id}")
    public LegalRequestDto getLegalRequestById(@PathVariable Long id) {
        return legalRequestService.getById(id);
    }

    @GetMapping("/all")
    public Page<LegalRequestDto> getLegalRequests(
            @RequestParam Long userId,
            @RequestParam String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return legalRequestService.getLegalRequests(userId, role, page, size);
    }

    @GetMapping("/AllFilter")
    public Page<LegalRequestDto> searchRequests(
            @RequestParam(required = false) LegalStatus status,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String milestoneName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size

    ) {
        return legalRequestService.searchRequests(
                status,
                projectId,
                assignedTo,
                createdBy,
                projectName,
                milestoneName,
                startDate,
                endDate,
                page,
                size
        );
    }
}
