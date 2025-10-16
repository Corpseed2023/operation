package com.doc.controller.department;

import com.doc.dto.team.TeamRequest;
import com.doc.service.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);

    @Autowired
    private TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamRequest> createTeam(@RequestBody TeamRequest teamRequest) {
        logger.info("API request to create team: {}", teamRequest.getName());
        TeamRequest createdTeam = teamService.createTeam(teamRequest);
        return ResponseEntity.ok(createdTeam);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamRequest> updateTeam(@PathVariable Long id, @RequestBody TeamRequest teamRequest) {
        logger.info("API request to update team ID: {}", id);
        TeamRequest updatedTeam = teamService.updateTeam(id, teamRequest);
        return ResponseEntity.ok(updatedTeam);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamRequest> getTeamById(@PathVariable Long id) {
        logger.info("API request to fetch team ID: {}", id);
        TeamRequest team = teamService.getTeamById(id);
        return ResponseEntity.ok(team);
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<TeamRequest>> getTeamsByDepartment(@PathVariable Long departmentId) {
        logger.info("API request to fetch teams for department ID: {}", departmentId);
        List<TeamRequest> teams = teamService.getTeamsByDepartment(departmentId);
        return ResponseEntity.ok(teams);
    }

    @GetMapping
    public ResponseEntity<Page<TeamRequest>> getAllTeams(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        logger.info("API request to fetch all teams, page: {}, size: {}", page, size);
        Page<TeamRequest> teams = teamService.getAllTeams(page, size);
        return ResponseEntity.ok(teams);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        logger.info("API request to delete team ID: {}", id);
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }
}