package com.doc.controller.legalrequest;
import com.doc.dto.LegalRequestDto.LegalRequestDto;
import com.doc.dto.LegalRequestDto.LegalStatusUpdateDto;
import com.doc.em.LegalStatus;
import com.doc.service.LegalRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/operationService/api/legal-request")
public class LegalRequestController {

    @Autowired
    private LegalRequestService legalRequestService;

    @PostMapping("/create")
    public LegalRequestDto createRequest(
            @RequestBody LegalRequestDto dto
    ) {
        return legalRequestService.createRequest(dto);
    }
    @PatchMapping("/{id}/status")
    public LegalRequestDto updateStatus(
            @PathVariable Long id,
            @RequestBody LegalStatusUpdateDto dto
    ) {
        return legalRequestService.updateStatus(id, dto);
    }
    @GetMapping("/{id}")
    public LegalRequestDto getLegalRequestById(@PathVariable Long id) {

        return legalRequestService.getById(id);
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
    @PatchMapping("/{id}/view")
    public LegalRequestDto markAsViewed(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        return legalRequestService.markAsViewed(id, userId);
    }

}
