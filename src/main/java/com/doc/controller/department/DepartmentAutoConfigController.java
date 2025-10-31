package com.doc.controller.department;

import com.doc.dto.DepartmentAutoConfigDto;
import com.doc.service.AutoAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing department auto-configuration settings for project assignment automation.
 */
@RestController
@RequestMapping("/api/department-auto-config")
@Validated
public class DepartmentAutoConfigController {

    @Autowired
    private AutoAssignmentService autoAssignmentService;

    @Operation(summary = "Update the auto-configuration settings for a department")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auto-configuration updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid configuration data"),
            @ApiResponse(responseCode = "404", description = "Department not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateAutoConfig(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentAutoConfigDto dto) {
        dto.setDepartmentId(id);
        autoAssignmentService.updateDepartmentAutoConfig(dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @Operation(summary = "Retrieve the auto-configuration settings for a department")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auto-configuration retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Department or configuration not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentAutoConfigDto> getAutoConfig(@PathVariable Long id) {
        DepartmentAutoConfigDto autoConfig = autoAssignmentService.getDepartmentAutoConfig(id);
        return new ResponseEntity<>(autoConfig, HttpStatus.OK);
    }
}