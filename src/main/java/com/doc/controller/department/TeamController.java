package com.doc.controller.department;

import com.doc.dto.team.TeamRequest;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.service.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing teams.
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);

    @Autowired
    private TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamRequest> createTeam(@RequestBody TeamRequest teamRequest) {
        logger.info("API request to create team: {}", teamRequest.getName());
        try {
            TeamRequest createdTeam = teamService.createTeam(teamRequest);
            logger.info("Team {} created successfully", createdTeam.getName());
            return ResponseEntity.ok(createdTeam);
        } catch (ValidationException e) {
            logger.error("Validation error creating team {}: {}", teamRequest.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found creating team {}: {}", teamRequest.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamRequest> updateTeam(@PathVariable Long id, @RequestBody TeamRequest teamRequest) {
        logger.info("API request to update team ID: {}", id);
        try {
            TeamRequest updatedTeam = teamService.updateTeam(id, teamRequest);
            logger.info("Team ID {} updated successfully", id);
            return ResponseEntity.ok(updatedTeam);
        } catch (ValidationException e) {
            logger.error("Validation error updating team ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found updating team ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamRequest> getTeamById(@PathVariable Long id) {
        logger.info("API request to fetch team ID: {}", id);
        try {
            TeamRequest team = teamService.getTeamById(id);
            logger.info("Team ID {} fetched successfully", id);
            return ResponseEntity.ok(team);
        } catch (ResourceNotFoundException e) {
            logger.error("Team ID {} not found: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<TeamRequest>> getTeamsByDepartment(@PathVariable Long departmentId) {
        logger.info("API request to fetch teams for department ID: {}", departmentId);
        try {
            List<TeamRequest> teams = teamService.getTeamsByDepartment(departmentId);
            logger.info("Fetched {} teams for department ID {}", teams.size(), departmentId);
            return ResponseEntity.ok(teams);
        } catch (ResourceNotFoundException e) {
            logger.error("Department ID {} not found: {}", departmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<Page<TeamRequest>> getAllTeams(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        logger.info("API request to fetch all teams, page: {}, size: {}", page, size);
        Page<TeamRequest> teams = teamService.getAllTeams(page, size);
        logger.info("Fetched {} teams across {} pages", teams.getTotalElements(), teams.getTotalPages());
        return ResponseEntity.ok(teams);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        logger.info("API request to delete team ID: {}", id);
        try {
            teamService.deleteTeam(id);
            logger.info("Team ID {} deleted successfully", id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            logger.error("Team ID {} not found: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}