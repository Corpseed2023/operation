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
import com.doc.repository.UserProductMapRepository;
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

/**
 * Service implementation for managing teams, including creation, update, and deletion.
 */
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

    @Autowired
    private UserProductMapRepository userProductMapRepository;

    @Override
    public TeamRequest createTeam(TeamRequest teamRequest) {
        logger.info("Creating team: {} for department ID: {}, temporary: {}", teamRequest.getName(), teamRequest.getDepartmentId(), teamRequest.isTemporary());

        // Validate input and user-product mappings
        validateTeamRequest(teamRequest, true);

        // Validate department
        Department department = departmentRepository.findByIdAndIsDeletedFalse(teamRequest.getDepartmentId())
                .orElseThrow(() -> {
                    logger.error("Department ID {} not found", teamRequest.getDepartmentId());
                    return new ResourceNotFoundException("Department not found", "ERR_DEPARTMENT_NOT_FOUND");
                });

        // Check for duplicate team name
        if (teamRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(teamRequest.getName(), teamRequest.getDepartmentId())) {
            logger.warn("Team name {} already exists in department ID {}", teamRequest.getName(), teamRequest.getDepartmentId());
            throw new ValidationException("Team name " + teamRequest.getName() + " already exists in department", "ERR_DUPLICATE_TEAM_NAME");
        }

        // Create and map team
        Team team = new Team();
        mapRequestToTeam(team, teamRequest);
        team.setDepartment(department);
        team.setCreatedDate(new Date());
        team.setUpdatedDate(new Date());
        team.setActive(true);
        team.setDeleted(false);
        team.setTemporary(teamRequest.isTemporary());
        team.setEndDate(teamRequest.isTemporary() ? teamRequest.getEndDate() : null);

        // Save team
        team = teamRepository.save(team);
        logger.info("Team created: {} (ID: {}) with {} members and {} products", team.getName(), team.getId(),
                team.getMembers().size(), team.getProducts().size());

        // Notify stakeholders (placeholder)
        logger.info("Notifying team lead ID {} and members for team {}", teamRequest.getTeamLeadId(), team.getName());
        // Example: notificationService.notifyTeamLeadAndMembers(team);

        return mapToRequest(team);
    }

    @Override
    public TeamRequest updateTeam(Long id, TeamRequest teamRequest) {
        logger.info("Updating team ID: {}", id);
        validateTeamRequest(teamRequest, false);

        // Fetch existing team
        Team team = teamRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.error("Team ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Team not found", "ERR_TEAM_NOT_FOUND");
                });

        // Prevent department change
        if (!team.getDepartment().getId().equals(teamRequest.getDepartmentId())) {
            logger.warn("Attempt to change department from {} to {}", team.getDepartment().getId(), teamRequest.getDepartmentId());
            throw new ValidationException("Cannot change department of an existing team", "ERR_INVALID_DEPARTMENT_CHANGE");
        }

        // Check for duplicate name
        if (!team.getName().equals(teamRequest.getName()) &&
                teamRepository.existsByNameAndDepartmentIdAndIsDeletedFalse(teamRequest.getName(), teamRequest.getDepartmentId())) {
            logger.warn("Team name {} already exists in department ID {}", teamRequest.getName(), teamRequest.getDepartmentId());
            throw new ValidationException("Team name " + teamRequest.getName() + " already exists in department", "ERR_DUPLICATE_TEAM_NAME");
        }

        // Update team
        mapRequestToTeam(team, teamRequest);
        team.setUpdatedDate(new Date());
        team = teamRepository.save(team);
        logger.info("Team updated: {} (ID: {}) with {} members and {} products", team.getName(), team.getId(),
                team.getMembers().size(), team.getProducts().size());

        // Notify stakeholders (placeholder)
        logger.info("Notifying team lead ID {} and members for updated team {}", teamRequest.getTeamLeadId(), team.getName());
        // Example: notificationService.notifyTeamLeadAndMembers(team);

        return mapToRequest(team);
    }

    @Override
    public TeamRequest getTeamById(Long id) {
        logger.info("Fetching team ID: {}", id);
        Team team = teamRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.error("Team ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Team not found", "ERR_TEAM_NOT_FOUND");
                });
        return mapToRequest(team);
    }

    @Override
    public List<TeamRequest> getTeamsByDepartment(Long departmentId) {
        logger.info("Fetching teams for department ID: {}", departmentId);
        departmentRepository.findByIdAndIsDeletedFalse(departmentId)
                .orElseThrow(() -> {
                    logger.error("Department ID {} not found", departmentId);
                    return new ResourceNotFoundException("Department not found", "ERR_DEPARTMENT_NOT_FOUND");
                });
        List<Team> teams = teamRepository.findByDepartmentIdAndIsDeletedFalse(departmentId);
        logger.debug("Found {} teams for department ID {}", teams.size(), departmentId);
        return teams.stream().map(this::mapToRequest).collect(Collectors.toList());
    }

    @Override
    public Page<TeamRequest> getAllTeams(int page, int size) {
        logger.info("Fetching all teams, page: {}, size: {}", page, size);
        Page<Team> teamPage = teamRepository.findAll(PageRequest.of(page, size));
        logger.debug("Found {} teams across {} pages", teamPage.getTotalElements(), teamPage.getTotalPages());
        return teamPage.map(this::mapToRequest);
    }

    @Override
    public void deleteTeam(Long id) {
        logger.info("Deleting team ID: {}", id);
        Team team = teamRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.error("Team ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Team not found", "ERR_TEAM_NOT_FOUND");
                });
        team.setDeleted(true);
        team.setUpdatedDate(new Date());
        teamRepository.save(team);
        logger.info("Team deleted: {} (ID: {})", team.getName(), id);

        // Notify stakeholders (placeholder)
        logger.info("Notifying team lead and members of team {} deletion", team.getName());
        // Example: notificationService.notifyTeamDeletion(team);
    }

    private void validateTeamRequest(TeamRequest teamRequest, boolean isCreate) {
        logger.debug("Validating team request for name: {}, department ID: {}, isCreate: {}", teamRequest.getName(), teamRequest.getDepartmentId(), isCreate);

        // Basic validations
        if (teamRequest.getName() == null || teamRequest.getName().trim().isEmpty()) {
            logger.warn("Team name is null or empty");
            throw new ValidationException("Team name cannot be empty", "ERR_INVALID_TEAM_NAME");
        }
        if (teamRequest.getDepartmentId() == null) {
            logger.warn("Department ID is null");
            throw new ValidationException("Department ID cannot be null", "ERR_NULL_DEPARTMENT_ID");
        }
        if (isCreate && teamRequest.getCreatedBy() == null) {
            logger.warn("Created by user ID is null");
            throw new ValidationException("Created by user ID cannot be null", "ERR_NULL_CREATED_BY");
        }
        if (teamRequest.getUpdatedBy() == null) {
            logger.warn("Updated by user ID is null");
            throw new ValidationException("Updated by user ID cannot be null", "ERR_NULL_UPDATED_BY");
        }

        // Validate ADMIN role for createdBy/updatedBy
        userRepository.findActiveUserById(teamRequest.getCreatedBy())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")))
                .orElseThrow(() -> {
                    logger.warn("Created by user ID {} is not ADMIN", teamRequest.getCreatedBy());
                    return new ValidationException("Only ADMIN can create teams", "ERR_UNAUTHORIZED_CREATE");
                });
        userRepository.findActiveUserById(teamRequest.getUpdatedBy())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")))
                .orElseThrow(() -> {
                    logger.warn("Updated by user ID {} is not ADMIN", teamRequest.getUpdatedBy());
                    return new ValidationException("Only ADMIN can update teams", "ERR_UNAUTHORIZED_UPDATE");
                });

        // Validate team lead and members
        if (teamRequest.getTeamLeadId() != null) {
            User teamLead = userRepository.findActiveUserById(teamRequest.getTeamLeadId())
                    .orElseThrow(() -> {
                        logger.warn("Team lead ID {} not found", teamRequest.getTeamLeadId());
                        return new ResourceNotFoundException("Team lead not found", "ERR_TEAM_LEAD_NOT_FOUND");
                    });
            if (!teamLead.getDepartments().stream().anyMatch(d -> d.getId().equals(teamRequest.getDepartmentId()))) {
                logger.warn("Team lead ID {} not in department ID {}", teamRequest.getTeamLeadId(), teamRequest.getDepartmentId());
                throw new ValidationException("Team lead must belong to the specified department", "ERR_INVALID_TEAM_LEAD");
            }
            if (teamRequest.getMemberIds() != null && !teamRequest.getMemberIds().contains(teamRequest.getTeamLeadId())) {
                logger.warn("Team lead ID {} not included in member IDs", teamRequest.getTeamLeadId());
                throw new ValidationException("Team lead must be a member of the team", "ERR_INVALID_TEAM_LEAD");
            }
        }

        // Validate members
        if (teamRequest.getMemberIds() != null) {
            for (Long id : teamRequest.getMemberIds()) {
                User member = userRepository.findActiveUserById(id)
                        .orElseThrow(() -> {
                            logger.warn("Member user ID {} not found", id);
                            return new ResourceNotFoundException("Member user ID " + id + " not found", "ERR_MEMBER_NOT_FOUND");
                        });
                if (!member.getDepartments().stream().anyMatch(d -> d.getId().equals(teamRequest.getDepartmentId()))) {
                    logger.warn("Member ID {} not in department ID {}", id, teamRequest.getDepartmentId());
                    throw new ValidationException("Member ID " + id + " must belong to the specified department", "ERR_INVALID_MEMBER");
                }
            }
        }

        // Validate products
        if (teamRequest.getProductIds() != null) {
            teamRequest.getProductIds().forEach(id ->
                    productRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(id)
                            .orElseThrow(() -> {
                                logger.warn("Product ID {} not found or inactive", id);
                                return new ResourceNotFoundException("Product ID " + id + " not found", "ERR_PRODUCT_NOT_FOUND");
                            }));
        }

        // Validate user-product mappings
        if (teamRequest.getMemberIds() != null && teamRequest.getProductIds() != null && !teamRequest.getProductIds().isEmpty()) {
            for (Long memberId : teamRequest.getMemberIds()) {
                User user = userRepository.findActiveUserById(memberId).orElseThrow();
                for (Long productId : teamRequest.getProductIds()) {
                    if (!userProductMapRepository.existsByUserIdAndProductIdAndIsDeletedFalse(memberId, productId)) {
                        logger.warn("User ID {} not mapped to product ID {}", memberId, productId);
                        throw new ValidationException("User " + user.getFullName() + " not mapped to product ID " + productId, "ERR_MISSING_USER_PRODUCT_MAPPING");
                    }
                }
                logger.debug("User ID {} mapped to all {} products", memberId, teamRequest.getProductIds().size());
            }
        }

        // Validate temporary team end date
        if (teamRequest.isTemporary() && teamRequest.getEndDate() == null) {
            logger.warn("Temporary team requires an end date");
            throw new ValidationException("End date required for temporary team", "ERR_NULL_END_DATE");
        }
        if (teamRequest.isTemporary() && teamRequest.getEndDate().before(new Date())) {
            logger.warn("End date {} is in the past", teamRequest.getEndDate());
            throw new ValidationException("End date for temporary team must be in the future", "ERR_INVALID_END_DATE");
        }
        logger.debug("Team request validated successfully");
    }

    private void mapRequestToTeam(Team team, TeamRequest teamRequest) {
        logger.debug("Mapping TeamRequest to Team for name: {}", teamRequest.getName());
        team.setName(teamRequest.getName().trim());
        team.setActive(teamRequest.isActive());
        team.setTemporary(teamRequest.isTemporary());
        team.setEndDate(teamRequest.isTemporary() ? teamRequest.getEndDate() : null);

        if (teamRequest.getTeamLeadId() != null) {
            User teamLead = userRepository.findActiveUserById(teamRequest.getTeamLeadId())
                    .orElseThrow(() -> {
                        logger.warn("Team lead ID {} not found", teamRequest.getTeamLeadId());
                        return new ResourceNotFoundException("Team lead not found", "ERR_TEAM_LEAD_NOT_FOUND");
                    });
            team.setTeamLead(teamLead);
        } else {
            team.setTeamLead(null);
        }

        if (teamRequest.getMemberIds() != null) {
            List<User> members = teamRequest.getMemberIds().stream()
                    .map(id -> userRepository.findActiveUserById(id)
                            .orElseThrow(() -> {
                                logger.warn("Member user ID {} not found", id);
                                return new ResourceNotFoundException("Member user ID " + id + " not found", "ERR_MEMBER_NOT_FOUND");
                            }))
                    .collect(Collectors.toList());
            team.setMembers(members);
        } else {
            team.setMembers(new ArrayList<>());
        }

        if (teamRequest.getProductIds() != null) {
            List<Product> products = teamRequest.getProductIds().stream()
                    .map(id -> productRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(id)
                            .orElseThrow(() -> {
                                logger.warn("Product ID {} not found or inactive", id);
                                return new ResourceNotFoundException("Product ID " + id + " not found", "ERR_PRODUCT_NOT_FOUND");
                            }))
                    .collect(Collectors.toList());
            team.setProducts(products);
        } else {
            team.setProducts(new ArrayList<>());
        }

        team.setCreatedBy(teamRequest.getCreatedBy());
        team.setUpdatedBy(teamRequest.getUpdatedBy());
        logger.debug("TeamRequest mapped to Team successfully");
    }

    private TeamRequest mapToRequest(Team team) {
        logger.debug("Mapping Team to TeamRequest for team ID: {}", team.getId());
        TeamRequest request = new TeamRequest();
        request.setName(team.getName());
        request.setDepartmentId(team.getDepartment().getId());
        request.setTeamLeadId(team.getTeamLead() != null ? team.getTeamLead().getId() : null);
        request.setMemberIds(team.getMembers().stream().map(User::getId).collect(Collectors.toList()));
        request.setProductIds(team.getProducts().stream().map(Product::getId).collect(Collectors.toList()));
        request.setActive(team.isActive());
        request.setTemporary(team.isTemporary());
        request.setEndDate(team.getEndDate());
        request.setCreatedBy(team.getCreatedBy());
        request.setUpdatedBy(team.getUpdatedBy());
        request.setCreatedDate(team.getCreatedDate());
        request.setUpdatedDate(team.getUpdatedDate());
        logger.debug("Team mapped to TeamRequest successfully");
        return request;
    }
}