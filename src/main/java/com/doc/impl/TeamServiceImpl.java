package com.doc.impl;

import com.doc.dto.team.TeamRequest;
import com.doc.entity.department.Department;
import com.doc.entity.department.Team;
import com.doc.entity.product.Product;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.DepartmentRepository;
import com.doc.repository.department.TeamRepository;
import com.doc.repository.ProductRepository;
import com.doc.repository.UserRepository;
import com.doc.service.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    private static final Logger logger = LoggerFactory.getLogger(TeamServiceImpl.class);

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public TeamRequest createTeam(TeamRequest teamRequest) {
        logger.info("Creating team: {} for department ID: {}", teamRequest.getName(), teamRequest.getDepartmentId());
        validateTeamRequest(teamRequest, true);

        Department department = departmentRepository.findByIdAndIsDeletedFalse(teamRequest.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found", "ERR_DEPARTMENT_NOT_FOUND"));

        if (teamRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(teamRequest.getName(), teamRequest.getDepartmentId())) {
            throw new ValidationException("Team name " + teamRequest.getName() + " already exists in department", "ERR_DUPLICATE_TEAM_NAME");
        }

        Team team = new Team();
        mapRequestToTeam(team, teamRequest);
        team.setDepartment(department);
        team.setCreatedDate(new Date());
        team.setUpdatedDate(new Date());
        team.setActive(true);
        team.setDeleted(false);

        team = teamRepository.save(team);
        logger.info("Team created: {} (ID: {})", team.getName(), team.getId());
        return mapToRequest(team);
    }

    @Override
    public TeamRequest updateTeam(Long id, TeamRequest teamRequest) {
        logger.info("Updating team ID: {}", id);
        validateTeamRequest(teamRequest, false);

        Team team = teamRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found", "ERR_TEAM_NOT_FOUND"));

        if (!team.getDepartment().getId().equals(teamRequest.getDepartmentId())) {
            throw new ValidationException("Cannot change department of an existing team", "ERR_INVALID_DEPARTMENT_CHANGE");
        }

        if (!team.getName().equals(teamRequest.getName()) &&
                teamRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(teamRequest.getName(), teamRequest.getDepartmentId())) {
            throw new ValidationException("Team name " + teamRequest.getName() + " already exists in department", "ERR_DUPLICATE_TEAM_NAME");
        }

        mapRequestToTeam(team, teamRequest);
        team.setUpdatedDate(new Date());
        team = teamRepository.save(team);
        logger.info("Team updated: {} (ID: {})", team.getName(), team.getId());
        return mapToRequest(team);
    }



    @Override
    public TeamRequest getTeamById(Long id) {
        logger.info("Fetching team ID: {}", id);
        Team team = teamRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found", "ERR_TEAM_NOT_FOUND"));
        return mapToRequest(team);
    }

    @Override
    public List<TeamRequest> getTeamsByDepartment(Long departmentId) {
        logger.info("Fetching teams for department ID: {}", departmentId);
        departmentRepository.findByIdAndIsDeletedFalse(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found", "ERR_DEPARTMENT_NOT_FOUND"));
        List<Team> teams = teamRepository.findByDepartmentIdAndIsDeletedFalse(departmentId);
        return teams.stream().map(this::mapToRequest).collect(Collectors.toList());
    }

    @Override
    public Page<TeamRequest> getAllTeams(int page, int size) {
        logger.info("Fetching all teams, page: {}, size: {}", page, size);
        Page<Team> teamPage = teamRepository.findAll(PageRequest.of(page, size));
        return teamPage.map(this::mapToRequest);
    }

    @Override
    public void deleteTeam(Long id) {
        logger.info("Deleting team ID: {}", id);
        Team team = teamRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found", "ERR_TEAM_NOT_FOUND"));
        team.setDeleted(true);
        team.setUpdatedDate(new Date());
        teamRepository.save(team);
        logger.info("Team deleted: {} (ID: {})", team.getName(), id);
    }

    private void validateTeamRequest(TeamRequest teamRequest, boolean isCreate) {
        if (teamRequest.getName() == null || teamRequest.getName().trim().isEmpty()) {
            throw new ValidationException("Team name cannot be empty", "ERR_INVALID_TEAM_NAME");
        }
        if (teamRequest.getDepartmentId() == null) {
            throw new ValidationException("Department ID cannot be null", "ERR_NULL_DEPARTMENT_ID");
        }
        if (isCreate && teamRequest.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null", "ERR_NULL_CREATED_BY");
        }
        if (teamRequest.getUpdatedBy() == null) {
            throw new ValidationException("Updated by user ID cannot be null", "ERR_NULL_UPDATED_BY");
        }
        // Validate ADMIN role for createdBy/updatedBy
        userRepository.findActiveUserById(teamRequest.getCreatedBy())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")))
                .orElseThrow(() -> new ValidationException("Only ADMIN can create teams", "ERR_UNAUTHORIZED_CREATE"));
        userRepository.findActiveUserById(teamRequest.getUpdatedBy())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")))
                .orElseThrow(() -> new ValidationException("Only ADMIN can update teams", "ERR_UNAUTHORIZED_UPDATE"));
        if (teamRequest.getTeamLeadId() != null) {
            User teamLead = userRepository.findActiveUserById(teamRequest.getTeamLeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team lead not found", "ERR_TEAM_LEAD_NOT_FOUND"));
            if (teamRequest.getMemberIds() != null && !teamRequest.getMemberIds().contains(teamRequest.getTeamLeadId())) {
                throw new ValidationException("Team lead must be a member of the team", "ERR_INVALID_TEAM_LEAD");
            }
        }
        if (teamRequest.getMemberIds() != null) {
            teamRequest.getMemberIds().forEach(id ->
                    userRepository.findActiveUserById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Member user ID " + id + " not found", "ERR_MEMBER_NOT_FOUND")));
        }
        if (teamRequest.getProductIds() != null) {
            teamRequest.getProductIds().forEach(id ->
                    productRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Product ID " + id + " not found", "ERR_PRODUCT_NOT_FOUND")));
        }
    }

    private void mapRequestToTeam(Team team, TeamRequest teamRequest) {
        team.setName(teamRequest.getName().trim());
        team.setActive(teamRequest.isActive());
        if (teamRequest.getTeamLeadId() != null) {
            User teamLead = userRepository.findActiveUserById(teamRequest.getTeamLeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team lead not found", "ERR_TEAM_LEAD_NOT_FOUND"));
            team.setTeamLead(teamLead);
        } else {
            team.setTeamLead(null);
        }
        if (teamRequest.getMemberIds() != null) {
            List<User> members = teamRequest.getMemberIds().stream()
                    .map(id -> userRepository.findActiveUserById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Member user ID " + id + " not found", "ERR_MEMBER_NOT_FOUND")))
                    .collect(Collectors.toList());
            team.setMembers(members);
        } else {
            team.setMembers(new ArrayList<>());
        }
        if (teamRequest.getProductIds() != null) {
            List<Product> products = teamRequest.getProductIds().stream()
                    .map(id -> productRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Product ID " + id + " not found", "ERR_PRODUCT_NOT_FOUND")))
                    .collect(Collectors.toList());
            team.setProducts(products);
        } else {
            team.setProducts(new ArrayList<>());
        }
        team.setCreatedBy(teamRequest.getCreatedBy());
        team.setUpdatedBy(teamRequest.getUpdatedBy());
    }

    private TeamRequest mapToRequest(Team team) {
        TeamRequest request = new TeamRequest();
        request.setName(team.getName());
        request.setDepartmentId(team.getDepartment().getId());
        request.setTeamLeadId(team.getTeamLead() != null ? team.getTeamLead().getId() : null);
        request.setMemberIds(team.getMembers().stream().map(User::getId).collect(Collectors.toList()));
        request.setProductIds(team.getProducts().stream().map(Product::getId).collect(Collectors.toList()));
        request.setActive(team.isActive());
        request.setCreatedBy(team.getCreatedBy());
        request.setUpdatedBy(team.getUpdatedBy());
        request.setCreatedDate(team.getCreatedDate());
        request.setUpdatedDate(team.getUpdatedDate());
        return request;
    }
}