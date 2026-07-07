package com.doc.impl.project;

import com.doc.dto.project.dashboard.ProjectStatusCountDto;
import com.doc.dto.project.dashboard.UserProjectDashboardResponseDto;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.service.project.UserProjectDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserProjectDashboardServiceImpl implements UserProjectDashboardService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    private static final List<String> ALL_STATUSES = List.of(
            "OPEN",
            "IN_PROGRESS",
            "COMPLETED",
            "CANCELLED",
            "REFUNDED",
            "REOPENED"
    );

    private static final List<String> RUNNING_STATUSES = List.of(
            "OPEN",
            "IN_PROGRESS",
            "REOPENED"
    );

    public UserProjectDashboardServiceImpl(
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProjectDashboardResponseDto getUserProjectDashboard(
            Long userId,
            Boolean currentMonth,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        Date fromDateTime = null;
        Date toDateTimeExclusive = null;

        boolean applyCurrentMonth = Boolean.TRUE.equals(currentMonth);

        if (applyCurrentMonth) {
            LocalDate today = LocalDate.now(INDIA_ZONE);
            fromDate = today.withDayOfMonth(1);
            toDate = today;
        }

        if (fromDate != null) {
            fromDateTime = Date.from(
                    fromDate.atStartOfDay(INDIA_ZONE).toInstant()
            );
        }

        if (toDate != null) {
            toDateTimeExclusive = Date.from(
                    toDate.plusDays(1).atStartOfDay(INDIA_ZONE).toInstant()
            );
        }

        boolean isAdmin = hasRole(user, "ADMIN");
        boolean isOperationHead = hasRole(user, "OPERATION_HEAD");

        Long totalProjects;
        Long runningProjects;
        List<ProjectStatusCountDto> rawStatusCounts;

        if (isAdmin || isOperationHead) {

            totalProjects = projectRepository.countAllProjectsForDashboardAdmin(
                    fromDateTime,
                    toDateTimeExclusive
            );

            runningProjects = projectRepository.countRunningProjectsForDashboardAdmin(
                    RUNNING_STATUSES,
                    fromDateTime,
                    toDateTimeExclusive
            );

            rawStatusCounts = projectRepository.getStatusCountsForDashboardAdmin(
                    fromDateTime,
                    toDateTimeExclusive
            );

        } else {

            List<Long> userIds = resolveAccessibleUserIds(user);

            totalProjects = projectRepository.countAllProjectsForDashboardUser(
                    userIds,
                    fromDateTime,
                    toDateTimeExclusive
            );

            runningProjects = projectRepository.countRunningProjectsForDashboardUser(
                    userIds,
                    RUNNING_STATUSES,
                    fromDateTime,
                    toDateTimeExclusive
            );

            rawStatusCounts = projectRepository.getStatusCountsForDashboardUser(
                    userIds,
                    fromDateTime,
                    toDateTimeExclusive
            );
        }

        Map<String, Long> countMap = rawStatusCounts.stream()
                .collect(Collectors.toMap(
                        dto -> normalizeStatus(dto.getStatus()),
                        ProjectStatusCountDto::getCount,
                        Long::sum
                ));

        List<ProjectStatusCountDto> statusCounts = ALL_STATUSES.stream()
                .map(status -> new ProjectStatusCountDto(
                        status,
                        countMap.getOrDefault(status, 0L)
                ))
                .toList();

        return new UserProjectDashboardResponseDto(
                userId,
                totalProjects,
                runningProjects,
                countMap.getOrDefault("OPEN", 0L),
                countMap.getOrDefault("IN_PROGRESS", 0L),
                countMap.getOrDefault("COMPLETED", 0L),
                countMap.getOrDefault("CANCELLED", 0L),
                countMap.getOrDefault("REFUNDED", 0L),
                countMap.getOrDefault("REOPENED", 0L),
                statusCounts
        );
    }

    private List<Long> resolveAccessibleUserIds(User user) {
        List<Long> userIds = new ArrayList<>();
        userIds.add(user.getId());

        if (user.isManagerFlag()) {
            List<User> subordinates =
                    userRepository.findByManagerIdAndIsDeletedFalse(user.getId());

            if (subordinates != null && !subordinates.isEmpty()) {
                userIds.addAll(
                        subordinates.stream()
                                .map(User::getId)
                                .toList()
                );
            }
        }

        return userIds;
    }

    private boolean hasRole(User user, String roleName) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }

        return user.getRoles().stream()
                .anyMatch(role ->
                        role.getName() != null
                                && role.getName().equalsIgnoreCase(roleName)
                );
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }
}