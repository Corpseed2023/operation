package com.doc.impl;

import com.doc.dto.project.ProjectResponseDto;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.service.ProjectSearchService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectSearchServiceImpl implements ProjectSearchService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private List<Long> getAccessibleUserIds(User user) {
        List<Long> userIds = new ArrayList<>();
        userIds.add(user.getId());
        if (user.isManagerFlag()) {
            List<User> subordinates = userRepository.findByManagerIdAndIsDeletedFalse(user.getId());
            userIds.addAll(subordinates.stream().map(User::getId).collect(Collectors.toList()));
        }
        return userIds;
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByCompanyName(String companyName, Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or deleted", "ERR_USER_NOT_FOUND"));
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findByCompanyNameContainingAndIsDeletedFalse(companyName);
        } else {
            List<Long> accessibleUserIds = getAccessibleUserIds(user);
            projects = projectRepository.findByCompanyNameContainingAndAssignedUserIdsAndIsDeletedFalse(companyName, accessibleUserIds);
        }
        return projects.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByProjectNumber(String projectNumber, Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or deleted", "ERR_USER_NOT_FOUND"));
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findByProjectNoContainingAndIsDeletedFalse(projectNumber);
        } else {
            List<Long> accessibleUserIds = getAccessibleUserIds(user);
            projects = projectRepository.findByProjectNoContainingAndAssignedUserIdsAndIsDeletedFalse(projectNumber, accessibleUserIds);
        }
        return projects.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByContactName(String contactName, Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or deleted", "ERR_USER_NOT_FOUND"));
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findByContactNameContainingAndIsDeletedFalse(contactName);
        } else {
            List<Long> accessibleUserIds = getAccessibleUserIds(user);
            projects = projectRepository.findByContactNameContainingAndAssignedUserIdsAndIsDeletedFalse(contactName, accessibleUserIds);
        }
        return projects.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponseDto> searchProjectsByProjectName(String projectName, Long userId) {
        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or deleted", "ERR_USER_NOT_FOUND"));
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findByNameContainingAndIsDeletedFalse(projectName);
        } else {
            List<Long> accessibleUserIds = getAccessibleUserIds(user);
            projects = projectRepository.findByNameContainingAndAssignedUserIdsAndIsDeletedFalse(projectName, accessibleUserIds);
        }
        return projects.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Maps Project entity to ProjectResponseDto
     * Uses salesPersonId and salesPersonName directly from Project (from microservice)
     */
    private ProjectResponseDto mapToResponseDto(Project project) {
        ProjectResponseDto dto = new ProjectResponseDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setProjectNo(project.getProjectNo());
        dto.setUnbilledNumber(project.getUnbilledNumber());
        dto.setEstimateNumber(project.getEstimateNumber());

        // FROM MICROSERVICE
        dto.setSalesPersonId(project.getSalesPersonId());
        dto.setSalesPersonName(project.getSalesPersonName());

        dto.setProductId(project.getProduct() != null ? project.getProduct().getId() : null);
        dto.setCompanyId(project.getCompany() != null ? project.getCompany().getId() : null);
        dto.setCompanyName(project.getCompany() != null ? project.getCompany().getName() : null);
        dto.setContactId(project.getContact() != null ? project.getContact().getId() : null);
        dto.setContactName(project.getContact() != null ? project.getContact().getName() : null);

        dto.setLeadId(project.getLeadId());
        dto.setDate(project.getDate());
        dto.setAddress(project.getAddress());
        dto.setCity(project.getCity());
        dto.setState(project.getState());
        dto.setCountry(project.getCountry());
        dto.setPrimaryPinCode(project.getPrimaryPinCode());

        dto.setTotalAmount(project.getPaymentDetail() != null ? project.getPaymentDetail().getTotalAmount() : 0.0);
        dto.setDueAmount(project.getPaymentDetail() != null ? project.getPaymentDetail().getDueAmount() : 0.0);
        dto.setPaymentTypeId(project.getPaymentDetail() != null && project.getPaymentDetail().getPaymentType() != null
                ? project.getPaymentDetail().getPaymentType().getId() : null);
        dto.setApprovedById(project.getPaymentDetail() != null && project.getPaymentDetail().getApprovedBy() != null
                ? project.getPaymentDetail().getApprovedBy().getId() : null);


        // ✅ ADD THIS BLOCK
        if (project.getStatus() != null) {
            dto.setStatusId(project.getStatus().getId());
            dto.setStatusName(project.getStatus().getName());

        }

        dto.setCreatedDate(project.getCreatedDate());
        dto.setUpdatedDate(project.getUpdatedDate());
        dto.setDeleted(project.isDeleted());
        dto.setActive(project.isActive());

        return dto;
    }

    @Override
    public List<ProjectResponseDto> searchProjects(String type, String value, Long userId) {

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found or deleted",
                                "ERR_USER_NOT_FOUND"
                        )
                );

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Project> query = cb.createQuery(Project.class);
        Root<Project> project = query.from(Project.class);

        List<Predicate> predicates = new ArrayList<>();

        // Always filter soft delete
        predicates.add(cb.isFalse(project.get("isDeleted")));

        // 🔹 Dynamic Search Condition
        String likeValue = "%" + value.toLowerCase() + "%";

        switch (type.toLowerCase()) {

            case "company":
                Join<Object, Object> companyJoin = project.join("company", JoinType.LEFT);
                predicates.add(
                        cb.like(
                                cb.lower(companyJoin.get("name")),
                                likeValue
                        )
                );
                break;

            case "project-number":
                predicates.add(
                        cb.like(
                                cb.lower(project.get("projectNo")),
                                likeValue
                        )
                );
                break;

            case "contact":
                Join<Object, Object> contactJoin = project.join("contact", JoinType.LEFT);
                predicates.add(
                        cb.like(
                                cb.lower(contactJoin.get("name")),
                                likeValue
                        )
                );
                break;

            case "project-name":
                predicates.add(
                        cb.like(
                                cb.lower(project.get("name")),
                                likeValue
                        )
                );
                break;

            default:
                throw new IllegalArgumentException("Invalid search type");
        }

        // 🔹 Role-Based Filtering (EXISTS subquery for non-admin)
        if (!isAdmin) {

            List<Long> accessibleUserIds = getAccessibleUserIds(user);

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ProjectMilestoneAssignment> assignment =
                    subquery.from(ProjectMilestoneAssignment.class);

            subquery.select(assignment.get("project").get("id"))
                    .where(
                            cb.and(
                                    cb.equal(
                                            assignment.get("project"),
                                            project
                                    ),
                                    assignment.get("assignedUser")
                                            .get("id")
                                            .in(accessibleUserIds),
                                    cb.isFalse(assignment.get("isDeleted"))
                            )
                    );

            predicates.add(cb.exists(subquery));
        }

        query.select(project)
                .where(cb.and(predicates.toArray(new Predicate[0])))
                .distinct(true);

        List<Project> projects =
                entityManager.createQuery(query).getResultList();

        return projects.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // ================= DTO MAPPER (UNCHANGED) =================


}