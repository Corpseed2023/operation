package com.doc.controller.legalDashboard;

import com.doc.dto.legalDashbaord.PendingLegalQueueItemDto;
import com.doc.impl.legalDashboard.LegalDashboardAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/legal-dashboard")
@RequiredArgsConstructor
public class LegalDashboardController {

    private final LegalDashboardAggregationService aggregationService;

    @GetMapping("/pending-queue")
    public ResponseEntity<List<PendingLegalQueueItemDto>> getPendingLegalQueue(
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(aggregationService.getPendingLegalQueue(userId));
    }
}