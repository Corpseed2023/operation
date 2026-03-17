package com.doc.controller.milestone;


import com.doc.service.MilestoneStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/operationService/api/milestone-statuses")
public class MilestoneStatusController {

    @Autowired
    private MilestoneStatusService milestoneStatusService;

    @Operation(summary = "Get all milestone statuses", description = "Returns list of id and name only")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    })
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllMilestoneStatuses() {
        return ResponseEntity.ok(milestoneStatusService.getAllMilestoneStatuses());
    }

    @Operation(summary = "Get milestone status by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status found"),
            @ApiResponse(responseCode = "404", description = "Status not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMilestoneStatusById(@PathVariable Long id) {
        return ResponseEntity.ok(milestoneStatusService.getMilestoneStatusById(id));
    }

    @Operation(summary = "Get milestone status by name (e.g., IN_PROGRESS)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status found"),
            @ApiResponse(responseCode = "404", description = "Status not found")
    })
    @GetMapping("/by-name/{name}")
    public ResponseEntity<Map<String, Object>> getMilestoneStatusByName(@PathVariable String name) {
        return ResponseEntity.ok(milestoneStatusService.getMilestoneStatusByName(name));
    }
}