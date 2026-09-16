package com.doc.impl;

import com.doc.dto.team.AssignmentRequest;
import com.doc.dto.team.AssignmentResponse;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing teams, including creation, update, deletion,
 * sub-team grouping, and round-robin assignment.
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

        // Validate input and user-product mappings (no currentTeamId — this is a create)
        validateTeamRequest(teamRequest, true, null);

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
        mapRequestToTeam(team, teamRequest, null);
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

        // If this is a group team and sub-team IDs were supplied at creation time, wire them up
        // in the given order (sequence = position in the list, starting at 1)
        if (team.isGroup() && teamRequest.getSubTeamIds() != null && !teamRequest.getSubTeamIds().isEmpty()) {
            int seq = 1;
            for (Long subTeamId : teamRequest.getSubTeamIds()) {
                addSubTeam(team.getId(), subTeamId, seq);
                seq++;
            }
            logger.info("Attached {} sub-teams to group team ID {}", teamRequest.getSubTeamIds().size(), team.getId());
        }

        // Notify stakeholders (placeholder)
        logger.info("Notifying team lead ID {} and members for team {}", teamRequest.getTeamLeadId(), team.getName());
        // Example: notificationService.notifyTeamLeadAndMembers(team);

        return mapToRequest(team);
    }

    @Override
    public TeamRequest updateTeam(Long id, TeamRequest teamRequest) {
        logger.info("Updating team ID: {}", id);

        // Fetch existing team first so we can pass its id into validation (excludes self
        // from the duplicate-sequence check)
        Team team = teamRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.error("Team ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("Team not found", "ERR_TEAM_NOT_FOUND");
                });

        validateTeamRequest(teamRequest, false, id);

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
        mapRequestToTeam(team, teamRequest, id);
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

        // Block deleting a group team that still has active sub-teams under it —
        // otherwise those sub-teams are left pointing at a soft-deleted parent
        if (team.isGroup()) {
            List<Team> activeSubTeams = teamRepository.findByParentTeamIdAndIsDeletedFalseOrderBySequenceAsc(id);
            if (!activeSubTeams.isEmpty()) {
                logger.warn("Cannot delete group team ID {} - {} active sub-team(s) still attached", id, activeSubTeams.size());
                throw new ValidationException("Cannot delete a group with active sub-teams. Remove sub-teams first.", "ERR_GROUP_HAS_SUBTEAMS");
            }
        }

        team.setDeleted(true);
        team.setUpdatedDate(new Date());
        teamRepository.save(team);
        logger.info("Team deleted: {} (ID: {})", team.getName(), id);

        // Notify stakeholders (placeholder)
        logger.info("Notifying team lead and members of team {} deletion", team.getName());
        // Example: notificationService.notifyTeamDeletion(team);
    }

    @Override
    public TeamRequest addSubTeam(Long groupTeamId, Long subTeamId, Integer sequence) {
        logger.info("Adding sub-team ID {} to group team ID {} at sequence {}", subTeamId, groupTeamId, sequence);

        if (sequence == null) {
            throw new ValidationException("Sequence cannot be null", "ERR_NULL_SEQUENCE");
        }

        Team group = teamRepository.findByIdAndIsGroupTrueAndIsDeletedFalse(groupTeamId)
                .orElseThrow(() -> {
                    logger.error("Group team ID {} not found", groupTeamId);
                    return new ResourceNotFoundException("Group team not found", "ERR_TEAM_NOT_FOUND");
                });
        Team sub = teamRepository.findByIdAndIsDeletedFalse(subTeamId)
                .orElseThrow(() -> {
                    logger.error("Sub-team ID {} not found", subTeamId);
                    return new ResourceNotFoundException("Sub-team not found", "ERR_TEAM_NOT_FOUND");
                });

        if (group.getId().equals(sub.getId())) {
            throw new ValidationException("A team cannot be its own sub-team", "ERR_SELF_REFERENCE_NOT_ALLOWED");
        }
        if (sub.isGroup()) {
            logger.warn("Attempt to nest group team ID {} inside group team ID {}", subTeamId, groupTeamId);
            throw new ValidationException("Cannot nest a group team as a sub-team", "ERR_NESTED_GROUP_NOT_ALLOWED");
        }
        // Exclude the sub-team's own current row so re-adding it at the same slot doesn't false-positive
        if (teamRepository.existsByParentTeamIdAndSequenceAndIdNotAndIsDeletedFalse(groupTeamId, sequence, subTeamId)) {
            logger.warn("Sequence {} already used in group team ID {}", sequence, groupTeamId);
            throw new ValidationException("Sequence already used in this group", "ERR_DUPLICATE_SEQUENCE");
        }

        sub.setParentTeam(group);
        sub.setSequence(sequence);
        sub.setUpdatedDate(new Date());
        teamRepository.save(sub);

        logger.info("Sub-team ID {} added to group team ID {} at sequence {}", subTeamId, groupTeamId, sequence);
        return mapToRequest(group);
    }

    @Override
    public TeamRequest removeSubTeam(Long groupTeamId, Long subTeamId) {
        logger.info("Removing sub-team ID {} from group team ID {}", subTeamId, groupTeamId);

        Team sub = teamRepository.findByIdAndIsDeletedFalse(subTeamId)
                .orElseThrow(() -> {
                    logger.error("Sub-team ID {} not found", subTeamId);
                    return new ResourceNotFoundException("Sub-team not found", "ERR_TEAM_NOT_FOUND");
                });

        if (sub.getParentTeam() == null || !sub.getParentTeam().getId().equals(groupTeamId)) {
            logger.warn("Team ID {} is not a sub-team of group ID {}", subTeamId, groupTeamId);
            throw new ValidationException("Team is not a sub-team of the specified group", "ERR_INVALID_PARENT");
        }

        sub.setParentTeam(null);
        sub.setSequence(null);
        sub.setUpdatedDate(new Date());
        teamRepository.save(sub);

        logger.info("Sub-team ID {} removed from group team ID {}", subTeamId, groupTeamId);
        return getTeamById(groupTeamId);
    }

    @Override
    public List<TeamRequest> getSubTeams(Long groupTeamId) {
        logger.info("Fetching sub-teams for group team ID: {}", groupTeamId);
        // Confirm the group exists (and is actually a group) before listing, for a clean 404
        teamRepository.findByIdAndIsGroupTrueAndIsDeletedFalse(groupTeamId)
                .orElseThrow(() -> {
                    logger.error("Group team ID {} not found", groupTeamId);
                    return new ResourceNotFoundException("Group team not found", "ERR_TEAM_NOT_FOUND");
                });
        List<Team> subTeams = teamRepository.findByParentTeamIdAndIsDeletedFalseOrderBySequenceAsc(groupTeamId);
        logger.debug("Found {} sub-teams for group team ID {}", subTeams.size(), groupTeamId);
        return subTeams.stream().map(this::mapToRequest).collect(Collectors.toList());
    }

    @Override
    public synchronized AssignmentResponse assignRoundRobin(AssignmentRequest request) {
        logger.info("Assigning round-robin for group team ID {} and product ID {}", request.getGroupTeamId(), request.getProductId());

        if (request.getGroupTeamId() == null || request.getProductId() == null) {
            throw new ValidationException("Group team ID and product ID are required", "ERR_NULL_ASSIGNMENT_INPUT");
        }

        Team group = teamRepository.findByIdAndIsGroupTrueAndIsDeletedFalse(request.getGroupTeamId())
                .orElseThrow(() -> {
                    logger.error("Group team ID {} not found", request.getGroupTeamId());
                    return new ResourceNotFoundException("Group team not found", "ERR_TEAM_NOT_FOUND");
                });

        // Confirm the product exists / is active, consistent with validation elsewhere
        productRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(request.getProductId())
                .orElseThrow(() -> {
                    logger.warn("Product ID {} not found or inactive", request.getProductId());
                    return new ResourceNotFoundException("Product ID " + request.getProductId() + " not found", "ERR_PRODUCT_NOT_FOUND");
                });

        List<Team> subTeams = teamRepository.findByParentTeamIdAndIsDeletedFalseOrderBySequenceAsc(group.getId());

        // Build ordered (user, sourceTeamId) pool
        List<Object[]> pool = new ArrayList<>(); // [User, Long sourceTeamId or null]
        if (!subTeams.isEmpty()) {
            logger.debug("Group team ID {} resolving via {} sub-team(s)", group.getId(), subTeams.size());
            for (Team t : subTeams) {
                t.getMembers().stream()
                        .sorted(Comparator.comparing(User::getId))
                        .filter(u -> userProductMapRepository.existsByUserIdAndProductIdAndIsDeletedFalse(u.getId(), request.getProductId()))
                        .forEach(u -> pool.add(new Object[]{u, t.getId()}));
            }
        } else {
            logger.debug("Group team ID {} has no sub-teams, resolving via direct members", group.getId());
            group.getMembers().stream()
                    .sorted(Comparator.comparing(User::getId))
                    .filter(u -> userProductMapRepository.existsByUserIdAndProductIdAndIsDeletedFalse(u.getId(), request.getProductId()))
                    .forEach(u -> pool.add(new Object[]{u, null}));
        }

        if (pool.isEmpty()) {
            logger.warn("No eligible members found for group team ID {} and product ID {}", group.getId(), request.getProductId());
            throw new ValidationException("No eligible members found for this product", "ERR_NO_ELIGIBLE_MEMBERS");
        }

        int nextIndex = 0;
        if (group.getLastAssignedUserId() != null) {
            int lastIdx = -1;
            for (int i = 0; i < pool.size(); i++) {
                if (((User) pool.get(i)[0]).getId().equals(group.getLastAssignedUserId())) {
                    lastIdx = i;
                    break;
                }
            }
            // wraps to 0 if lastIdx not found (previous user left the pool) or was at the end
            nextIndex = (lastIdx + 1) % pool.size();
        }

        User assignedUser = (User) pool.get(nextIndex)[0];
        Long sourceTeamId = (Long) pool.get(nextIndex)[1];

        group.setLastAssignedUserId(assignedUser.getId());
        group.setUpdatedDate(new Date());
        teamRepository.save(group);

        logger.info("Assigned user ID {} ({}) from team ID {} for group team ID {}, product ID {}",
                assignedUser.getId(), assignedUser.getFullName(), sourceTeamId, group.getId(), request.getProductId());

        AssignmentResponse response = new AssignmentResponse();
        response.setAssignedUserId(assignedUser.getId());
        response.setAssignedUserName(assignedUser.getFullName());
        response.setResolvedFromTeamId(sourceTeamId);
        return response;
    }

    /**
     * @param currentTeamId the ID of the team being updated, or null when this is a create.
     *                       Used to exclude the team's own row from the duplicate-sequence check
     *                       so re-saving an unchanged sub-team doesn't falsely collide with itself.
     */
    private void validateTeamRequest(TeamRequest teamRequest, boolean isCreate, Long currentTeamId) {
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

        // A sub-team cannot itself be a group
        if (teamRequest.getParentTeamId() != null && teamRequest.isGroup()) {
            logger.warn("Team request attempts to be both a sub-team and a group");
            throw new ValidationException("A sub-team cannot itself be a routing group", "ERR_NESTED_GROUP_NOT_ALLOWED");
        }

        // Sequence required if this is a sub-team
        if (teamRequest.getParentTeamId() != null && teamRequest.getSequence() == null) {
            logger.warn("Sequence missing for sub-team with parent team ID {}", teamRequest.getParentTeamId());
            throw new ValidationException("Sequence required when assigning a parent team", "ERR_NULL_SEQUENCE");
        }

        // A team cannot be its own parent
        if (teamRequest.getParentTeamId() != null && currentTeamId != null &&
                teamRequest.getParentTeamId().equals(currentTeamId)) {
            logger.warn("Team ID {} attempted to set itself as its own parent", currentTeamId);
            throw new ValidationException("A team cannot be its own parent", "ERR_SELF_REFERENCE_NOT_ALLOWED");
        }

        // No duplicate sequence within same parent (excluding this team's own current row on update)
        if (teamRequest.getParentTeamId() != null) {
            boolean duplicate = currentTeamId != null
                    ? teamRepository.existsByParentTeamIdAndSequenceAndIdNotAndIsDeletedFalse(
                    teamRequest.getParentTeamId(), teamRequest.getSequence(), currentTeamId)
                    : teamRepository.existsByParentTeamIdAndSequenceAndIsDeletedFalse(
                    teamRequest.getParentTeamId(), teamRequest.getSequence());
            if (duplicate) {
                logger.warn("Sequence {} already used under parent team ID {}", teamRequest.getSequence(), teamRequest.getParentTeamId());
                throw new ValidationException("Sequence already used in this group", "ERR_DUPLICATE_SEQUENCE");
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

    /**
     * @param currentTeamId the ID of the team being mapped (already persisted), or null on create.
     *                       Passed through so a self-parent reference can be caught defensively here too.
     */
    private void mapRequestToTeam(Team team, TeamRequest teamRequest, Long currentTeamId) {
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

        team.setGroup(teamRequest.isGroup());
        team.setSequence(teamRequest.getSequence());

        if (teamRequest.getParentTeamId() != null) {
            if (currentTeamId != null && teamRequest.getParentTeamId().equals(currentTeamId)) {
                // defensive re-check; validateTeamRequest should already have caught this
                logger.warn("Team ID {} attempted to set itself as its own parent during mapping", currentTeamId);
                throw new ValidationException("A team cannot be its own parent", "ERR_SELF_REFERENCE_NOT_ALLOWED");
            }
            Team parent = teamRepository.findByIdAndIsGroupTrueAndIsDeletedFalse(teamRequest.getParentTeamId())
                    .orElseThrow(() -> {
                        logger.warn("Parent group team ID {} not found", teamRequest.getParentTeamId());
                        return new ResourceNotFoundException("Parent group team not found", "ERR_PARENT_TEAM_NOT_FOUND");
                    });
            team.setParentTeam(parent);
        } else {
            team.setParentTeam(null);
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
        request.setParentTeamId(team.getParentTeam() != null ? team.getParentTeam().getId() : null);
        request.setSequence(team.getSequence());
        request.setGroup(team.isGroup());
        request.setCreatedDate(team.getCreatedDate());
        request.setUpdatedDate(team.getUpdatedDate());
        logger.debug("Team mapped to TeamRequest successfully");
        return request;
    }
}