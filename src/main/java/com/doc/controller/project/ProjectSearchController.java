package com.doc.controller.project;

import com.doc.dto.project.ProjectResponseDto;
import com.doc.service.ProjectSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/projects/search")
public class ProjectSearchController {

    @Autowired
    private ProjectSearchService projectSearchService;

    @Operation(summary = "Search projects by company name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projects found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "User not found or deleted")
    })
    @GetMapping("/by-company")
    public ResponseEntity<List<ProjectResponseDto>> searchByCompanyName(
            @RequestParam String companyName,
            @RequestParam Long userId) {
        List<ProjectResponseDto> projects = projectSearchService.searchProjectsByCompanyName(companyName, userId);
        return ResponseEntity.ok(projects);
    }

    @Operation(summary = "Search projects by project number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projects found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "User not found or deleted")
    })
    @GetMapping("/by-project-number")
    public ResponseEntity<List<ProjectResponseDto>> searchByProjectNumber(
            @RequestParam String projectNumber,
            @RequestParam Long userId) {
        List<ProjectResponseDto> projects = projectSearchService.searchProjectsByProjectNumber(projectNumber, userId);
        return ResponseEntity.ok(projects);
    }

    @Operation(summary = "Search projects by contact name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projects found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "User not found or deleted")
    })
    @GetMapping("/by-contact-name")
    public ResponseEntity<List<ProjectResponseDto>> searchByContactName(
            @RequestParam String contactName,
            @RequestParam Long userId) {
        List<ProjectResponseDto> projects = projectSearchService.searchProjectsByContactName(contactName, userId);
        return ResponseEntity.ok(projects);
    }

    @Operation(summary = "Search projects by project name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projects found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "User not found or deleted")
    })
    @GetMapping("/by-project-name")
    public ResponseEntity<List<ProjectResponseDto>> searchByProjectName(
            @RequestParam String projectName,
            @RequestParam Long userId) {
        List<ProjectResponseDto> projects = projectSearchService.searchProjectsByProjectName(projectName, userId);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{type}")
    public ResponseEntity<List<ProjectResponseDto>> searchProjects(
            @PathVariable String type,
            @RequestParam String value,
            @RequestParam Long userId,
            @RequestParam(required = false) String statusName,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        List<ProjectResponseDto> projects =
                projectSearchService.searchProjects(type, value, userId,statusName, fromDate, toDate);

        return ResponseEntity.ok(projects);
    }

}