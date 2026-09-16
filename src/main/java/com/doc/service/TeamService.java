package com.doc.service;

import com.doc.dto.team.AssignmentRequest;
import com.doc.dto.team.AssignmentResponse;
import com.doc.dto.team.TeamRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TeamService {

    TeamRequest createTeam(TeamRequest teamRequest);

    TeamRequest updateTeam(Long id, TeamRequest teamRequest);

    TeamRequest getTeamById(Long id);

    List<TeamRequest> getTeamsByDepartment(Long departmentId);

    Page<TeamRequest> getAllTeams(int page, int size);

    void deleteTeam(Long id);

    TeamRequest addSubTeam(Long groupTeamId, Long subTeamId, Integer sequence);

    TeamRequest removeSubTeam(Long groupTeamId, Long subTeamId);

    List<TeamRequest> getSubTeams(Long groupTeamId);

    AssignmentResponse assignRoundRobin(AssignmentRequest request);
}