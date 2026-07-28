package com.doc.impl.project;

import com.doc.dto.project.dashboard.ProjectOverviewCardDto;
import com.doc.dto.project.dashboard.ProjectOverviewResponseDto;
import com.doc.dto.project.dashboard.ProjectStatusCountDto;
import com.doc.dto.project.dashboard.UserProjectDashboardResponseDto;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.projection.VendorAssignmentCountProjection;
import com.doc.service.project.ProjectDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectDashboardServiceImpl implements ProjectDashboardService {

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

    private static final List<String> IN_PROGRESS_STATUSES = List.of(
            "IN_PROGRESS"
    );

    private static final List<String> AWAITING_DOCUMENT_STATUSES = List.of(
            "OPEN",
            "REOPENED"
    );

    public ProjectDashboardServiceImpl(
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

        User user = getActiveUser(userId);

        DateRange dateRange = resolveDateRange(currentMonth, fromDate, toDate);

        boolean isAdmin = hasRole(user, "ADMIN");
        boolean isOperationHead = hasRole(user, "OPERATION_HEAD");

        Long totalProjects;
        Long runningProjects;
        List<ProjectStatusCountDto> rawStatusCounts;

        if (isAdmin || isOperationHead) {

            totalProjects = projectRepository.countAllProjectsForDashboardAdmin(
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

            runningProjects = projectRepository.countRunningProjectsForDashboardAdmin(
                    RUNNING_STATUSES,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

            rawStatusCounts = projectRepository.getStatusCountsForDashboardAdmin(
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

        } else {

            List<Long> userIds = resolveAccessibleUserIds(user);

            totalProjects = projectRepository.countAllProjectsForDashboardUser(
                    userIds,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

            runningProjects = projectRepository.countRunningProjectsForDashboardUser(
                    userIds,
                    RUNNING_STATUSES,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );



            rawStatusCounts = projectRepository.getStatusCountsForDashboardUser(
                    userIds,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
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

    @Override
    @Transactional(readOnly = true)
    public ProjectOverviewResponseDto getProjectOverview(
            Long userId,
            Boolean currentMonth,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        User user = getActiveUser(userId);

        DateRange dateRange = resolveDateRange(currentMonth, fromDate, toDate);

        LocalDate today = LocalDate.now(INDIA_ZONE);

        boolean isAdmin = hasRole(user, "ADMIN");
        boolean isOperationHead = hasRole(user, "OPERATION_HEAD");

        long totalProjects;
        long inProgressCount;
        long awaitingDocumentsCount;
        long delayedCount;

        if (isAdmin || isOperationHead) {

            totalProjects = projectRepository.countOverviewTotalAdmin(
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

            inProgressCount = projectRepository.countOverviewNonDelayedByStatusesAdmin(
                    IN_PROGRESS_STATUSES,
                    today,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

            awaitingDocumentsCount = projectRepository.countOverviewNonDelayedByStatusesAdmin(
                    AWAITING_DOCUMENT_STATUSES,
                    today,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

            delayedCount = projectRepository.countOverviewDelayedAdmin(
                    RUNNING_STATUSES,
                    today,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

        } else {

            List<Long> userIds = resolveAccessibleUserIds(user);

            totalProjects = projectRepository.countOverviewTotalUser(
                    userIds,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

            inProgressCount = projectRepository.countOverviewNonDelayedByStatusesUser(
                    userIds,
                    IN_PROGRESS_STATUSES,
                    today,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

            awaitingDocumentsCount = projectRepository.countOverviewNonDelayedByStatusesUser(
                    userIds,
                    AWAITING_DOCUMENT_STATUSES,
                    today,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );

            delayedCount = projectRepository.countOverviewDelayedUser(
                    userIds,
                    RUNNING_STATUSES,
                    today,
                    dateRange.fromDateTime(),
                    dateRange.toDateTimeExclusive()
            );
        }

        List<ProjectOverviewCardDto> cards = List.of(
                new ProjectOverviewCardDto(
                        "IN_PROGRESS",
                        "In Progress",
                        "Currently being worked on",
                        inProgressCount,
                        calculatePercentage(inProgressCount, totalProjects)
                ),
                new ProjectOverviewCardDto(
                        "AWAITING_DOCUMENTS",
                        "Awaiting Documents",
                        "Waiting for Docs/Info",
                        awaitingDocumentsCount,
                        calculatePercentage(awaitingDocumentsCount, totalProjects)
                ),
                new ProjectOverviewCardDto(
                        "DELAYED",
                        "Delayed",
                        "Past target date",
                        delayedCount,
                        calculatePercentage(delayedCount, totalProjects)
                )
        );

        return new ProjectOverviewResponseDto(
                userId,
                dateRange.currentMonthApplied(),
                dateRange.fromDate(),
                dateRange.toDate(),
                totalProjects,
                cards
        );
    }



    private User getActiveUser(Long userId) {
        return userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));
    }

    private DateRange resolveDateRange(
            Boolean currentMonth,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        boolean applyCurrentMonth = Boolean.TRUE.equals(currentMonth);

        if (applyCurrentMonth) {
            LocalDate today = LocalDate.now(INDIA_ZONE);
            fromDate = today.withDayOfMonth(1);
            toDate = today;
        }

        Date fromDateTime = null;
        Date toDateTimeExclusive = null;

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

        return new DateRange(
                applyCurrentMonth,
                fromDate,
                toDate,
                fromDateTime,
                toDateTimeExclusive
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

    private int calculatePercentage(long count, long total) {
        if (total <= 0) {
            return 0;
        }

        return (int) Math.floor((count * 100.0) / total);
    }

    private record DateRange(
            boolean currentMonthApplied,
            LocalDate fromDate,
            LocalDate toDate,
            Date fromDateTime,
            Date toDateTimeExclusive
    ) {
    }
}