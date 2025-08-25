package com.doc.controller.user;

import com.doc.dto.user.UserLoginStatusResponseDto;
import com.doc.service.UserLoginStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserLoginStatusController {

    @Autowired
    private UserLoginStatusService userLoginStatusService;

    @Operation(summary = "Set user to online")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User set to online successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{id}/online")
    public ResponseEntity<UserLoginStatusResponseDto> setOnline(@PathVariable Long id) {
        UserLoginStatusResponseDto dto = userLoginStatusService.setOnline(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Set user to offline")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User set to offline successfully"),
            @ApiResponse(responseCode = "404", description = "User online status not found")
    })
    @PostMapping("/{id}/offline")
    public ResponseEntity<UserLoginStatusResponseDto> setOffline(@PathVariable Long id) {
        UserLoginStatusResponseDto dto = userLoginStatusService.setOffline(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get user online status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User online status not found")
    })
    @GetMapping("/{id}/status")
    public ResponseEntity<UserLoginStatusResponseDto> getStatus(@PathVariable Long id) {
        UserLoginStatusResponseDto dto = userLoginStatusService.getStatus(id);
        return ResponseEntity.ok(dto);
    }
}