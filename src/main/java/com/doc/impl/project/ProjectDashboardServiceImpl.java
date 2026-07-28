package com.doc.impl.project;

import com.doc.constants.DepartmentConstants;
import com.doc.dto.project.dashboard.*;
import com.doc.dto.user.UserProjectPerformanceDetailDto;
import com.doc.dto.user.UserProjectPerformanceResponseDto;
import com.doc.entity.project.ProjectPriority;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.*;
import com.doc.repository.documentRepo.ProjectDocumentUploadRepository;
import com.doc.service.project.ProjectDashboardService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectDashboardServiceImpl implements ProjectDashboardService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private final ProjectMilestoneAssignmentRepository
            milestoneAssignmentRepository;

    private final ProjectDocumentUploadRepository
            projectDocumentUploadRepository;


    private final ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;

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
            UserRepository userRepository, ProjectMilestoneAssignmentRepository milestoneAssignmentRepository, ProjectDocumentUploadRepository projectDocumentUploadRepository, ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.milestoneAssignmentRepository = milestoneAssignmentRepository;
        this.projectDocumentUploadRepository = projectDocumentUploadRepository;
        this.projectMilestoneAssignmentRepository = projectMilestoneAssignmentRepository;
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

    @Override
    public ProjectCompletionResponseDto getProjectCompletionSummary(
            Long userId
    ) {

        //validateDepartmentAccess(userId);
        Long departmentId =
                validateUserAndGetDepartmentId(userId);

        ProjectCompletionProjection projection =
                projectRepository.getProjectCompletionSummary(departmentId);

        long totalProjectCount =
                projection == null
                        || projection.getTotalProjectCount() == null
                        ? 0L
                        : projection.getTotalProjectCount();

        long completedProjectCount =
                projection == null
                        || projection.getCompletedProjectCount() == null
                        ? 0L
                        : projection.getCompletedProjectCount();

        BigDecimal completionPercentage =
                calculateProjectCompletionPercentage(
                        completedProjectCount,
                        totalProjectCount
                );

        return ProjectCompletionResponseDto.builder()
                .totalProjectCount(totalProjectCount)
                .completedProjectCount(completedProjectCount)
                .completionPercentage(completionPercentage)
                .build();
    }

    @Override
    public List<ProjectStatusCountResponseDto>
    getProjectStatusWiseSummary(Long userId) {

        //validateDepartmentAccess(userId);
        Long departmentId =
                validateUserAndGetDepartmentId(userId);
        List<ProjectStatusCountProjection> projections =
                projectRepository.getProjectStatusWiseCount(departmentId);

        long totalProjectCount = projections.stream()
                .map(ProjectStatusCountProjection::getProjectCount)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        return projections.stream()
                .map(projection -> {

                    long projectCount =
                            projection.getProjectCount() == null
                                    ? 0L
                                    : projection.getProjectCount();

                    return ProjectStatusCountResponseDto.builder()
                            .statusId(projection.getStatusId())
                            .statusName(projection.getStatusName())
                            .projectCount(projectCount)
                            .percentage(
                                    calculateStatusPercentage(
                                            projectCount,
                                            totalProjectCount
                                    )
                            )
                            .build();
                })
                .toList();
    }

    @Override
    public List<MilestoneOverviewResponseDto> getMilestoneOverview(
            Long userId
    ) {

        //validateDepartmentAccess(userId);
        Long departmentId =
                validateUserAndGetDepartmentId(userId);

        List<MilestoneOverviewProjection> projections =
                projectMilestoneAssignmentRepository.getMilestoneOverview(departmentId);

        return projections.stream()
                .map(projection -> {

                    long totalProjects =
                            projection.getTotalProjects() == null
                                    ? 0L
                                    : projection.getTotalProjects();

                    long completedProjects =
                            projection.getCompletedProjects() == null
                                    ? 0L
                                    : projection.getCompletedProjects();

                    BigDecimal completionPercentage =
                            calculateMilestonePercentage(
                                    completedProjects,
                                    totalProjects
                            );

                    return MilestoneOverviewResponseDto.builder()
                            .milestoneId(projection.getMilestoneId())
                            .milestoneName(projection.getMilestoneName())
                            .totalProjects(totalProjects)
                            .completedProjects(completedProjects)
                            .completionPercentage(completionPercentage)
                            .build();
                })
                .toList();
    }

    @Override
    public List<TeamWorkloadResponseDto> getTeamWorkload(Long userId) {

        //validateDepartmentAccess(userId);
        Long departmentId =
                validateUserAndGetDepartmentId(userId);

        List<TeamWorkloadProjection> projections =
                projectMilestoneAssignmentRepository.getTeamWorkload(departmentId);

        return projections.stream()
                .map(projection -> {

                    long assignedCount =
                            projection.getAssignedCount() == null
                                    ? 0L
                                    : projection.getAssignedCount();

                    long completedCount =
                            projection.getCompletedCount() == null
                                    ? 0L
                                    : projection.getCompletedCount();

                    BigDecimal completionPercentage =
                            calculateTeamCompletionPercentage(
                                    completedCount,
                                    assignedCount
                            );

                    return TeamWorkloadResponseDto.builder()
                            .departmentId(projection.getDepartmentId())
                            .departmentName(
                                    projection.getDepartmentName() + " Team"
                            )
                            .assignedCount(assignedCount)
                            .completedCount(completedCount)
                            .completionPercentage(completionPercentage)
                            .build();
                })
                .toList();
    }

    @Override
    public List<DueRiskQueueResponseDto> getDueRiskQueue(
            Long userId,
            Integer upcomingDays,
            Integer limit
    ) {

        //validateDepartmentAccess(userId);
        Long departmentId =
                validateUserAndGetDepartmentId(userId);

        int days = upcomingDays == null || upcomingDays < 0
                ? 7
                : upcomingDays;

        int recordLimit = limit == null || limit <= 0
                ? 5
                : Math.min(limit, 100);

        LocalDate today = LocalDate.now();

        List<DueRiskQueueProjection> projections =
                projectMilestoneAssignmentRepository.findDueRiskQueue(departmentId,
                        days,
                        recordLimit
                );

        return projections.stream()
                .map(record -> {

                    LocalDate dueDate = record.getDueDate();

                    boolean overdue =
                            dueDate != null && dueDate.isBefore(today);

                    long overdueDays =
                            overdue
                                    ? ChronoUnit.DAYS.between(dueDate, today)
                                    : 0L;

                    ProjectPriority priority = null;

                    if (record.getPriority() != null) {
                        priority = ProjectPriority.valueOf(
                                String.valueOf(record.getPriority())
                        );
                    }

                    return DueRiskQueueResponseDto.builder()
                            .projectId(record.getProjectId())
                            .companyName(record.getCompanyName())
                            .projectNumber(record.getProjectNumber())
                            .milestoneId(record.getMilestoneId())
                            .milestoneName(record.getMilestoneName())
                            .dueDate(dueDate)
                            .ownerId(record.getOwnerId())
                            .ownerName(
                                    record.getOwnerName() != null
                                            ? record.getOwnerName()
                                            : "Unassigned"
                            )
                            .priority(priority)
                            .overdue(overdue)
                            .overdueDays(overdueDays)
                            .build();
                })
                .toList();
    }



    private BigDecimal calculateTeamCompletionPercentage(
            long completedCount,
            long assignedCount
    ) {

        if (assignedCount <= 0L) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(assignedCount),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateMilestonePercentage(
            long completedProjects,
            long totalProjects
    ) {

        if (totalProjects <= 0L) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return BigDecimal.valueOf(completedProjects)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(totalProjects),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateStatusPercentage(
            long statusProjectCount,
            long totalProjectCount
    ) {

        if (totalProjectCount <= 0L) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return BigDecimal.valueOf(statusProjectCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(totalProjectCount),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private void validateDepartmentAccess(Long userId) {

        if (userId == null) {
            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        Long userCount =
                userRepository.countActiveUserInDepartment(
                        userId,
                        DepartmentConstants.PROJECT_DEPARTMENT_ID
                );

        if (userCount == null || userCount <= 0L) {
            throw new ResourceNotFoundException(
                    "User is not authorized to access project resources",
                    "ERR_PROJECT_DEPARTMENT_ACCESS_DENIED"
            );
        }
    }



    private BigDecimal calculateProjectCompletionPercentage(
            long completedProjectCount,
            long totalProjectCount
    ) {

        if (totalProjectCount <= 0L) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return BigDecimal.valueOf(completedProjectCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(totalProjectCount),
                        2,
                        RoundingMode.HALF_UP
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

    @Override
    public Page<ProjectMilestoneTrackerResponseDto> getMilestoneTracker(
            Long userId,
            Long stageId,
            String search,
            int page,
            int size
    ) {

        Long departmentId =
                validateUserAndGetDepartmentId(userId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "id"
                )
        );

        String normalizedSearch =
                search == null || search.isBlank()
                        ? null
                        : search.trim();

        Page<ProjectTrackerSummaryProjection> projectPage =
                projectRepository.findProjectMilestoneTrackerProjects(
                        departmentId,
                        stageId,
                        normalizedSearch,
                        pageable
                );

        if (projectPage.isEmpty()) {
            return new PageImpl<>(
                    Collections.emptyList(),
                    pageable,
                    0
            );
        }

        List<Long> projectIds =
                projectPage.getContent()
                        .stream()
                        .map(
                                ProjectTrackerSummaryProjection::getProjectId
                        )
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        Map<Long, List<ProjectMilestoneTrackerProjection>>
                milestoneMap =
                fetchMilestoneMap(
                        projectIds,
                        departmentId
                );

        Map<Long, Long> pendingDocumentMap =
                fetchPendingDocumentMap(
                        projectIds
                );

        List<ProjectMilestoneTrackerResponseDto> response =
                projectPage.getContent()
                        .stream()
                        .map(project -> buildResponse(
                                project,
                                milestoneMap.getOrDefault(
                                        project.getProjectId(),
                                        Collections.emptyList()
                                ),
                                pendingDocumentMap.getOrDefault(
                                        project.getProjectId(),
                                        0L
                                )
                        ))
                        .toList();

        return new PageImpl<>(
                response,
                pageable,
                projectPage.getTotalElements()
        );
    }

    @Override
    public UserProjectPerformanceResponseDto getUserProjectPerformance(
            Long userId,
            Long projectId
    ) {

        if (userId == null) {
            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with ID: " + userId,
                        "ERR_ACTIVE_USER_NOT_FOUND"
                ));

        List<UserMilestonePerformanceProjection> records =
                projectMilestoneAssignmentRepository
                        .findUserProjectPerformance(userId, projectId);

        Map<Long, List<UserMilestonePerformanceProjection>> projectGroups =
                records.stream()
                        .filter(record -> record.getProjectId() != null)
                        .collect(Collectors.groupingBy(
                                UserMilestonePerformanceProjection::getProjectId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<UserProjectPerformanceDetailDto> projectDetails =
                new ArrayList<>();

        long totalCompletedMilestones = 0L;
        long totalBeforeTat = 0L;
        long totalWithinTat = 0L;
        long totalDelayed = 0L;

        /*
         * Number of completed milestones for which performance
         * was actually calculated.
         */
        long totalPerformanceCalculatedMilestones = 0L;

        BigDecimal totalPerformance = BigDecimal.ZERO;

        for (Map.Entry<Long, List<UserMilestonePerformanceProjection>> entry
                : projectGroups.entrySet()) {

            List<UserMilestonePerformanceProjection> projectRecords =
                    entry.getValue();

            if (projectRecords.isEmpty()) {
                continue;
            }

            UserMilestonePerformanceProjection first =
                    projectRecords.get(0);

            long projectCompletedMilestones = 0L;
            long beforeTatCount = 0L;
            long withinTatCount = 0L;
            long delayedCount = 0L;

            long projectPerformanceCalculatedMilestones = 0L;

            BigDecimal projectPerformanceTotal = BigDecimal.ZERO;

            for (UserMilestonePerformanceProjection record : projectRecords) {

                /*
                 * Count completed milestones independently of whether
                 * performance TAT is configured.
                 */
                if (isCompletedMilestone(record)) {
                    projectCompletedMilestones++;
                    totalCompletedMilestones++;
                }

                /*
                 * Do not calculate performance when:
                 * - milestone is not completed
                 * - TAT is not applicable
                 * - TAT hours are null or zero
                 * - started/completed dates are null
                 */
                if (!isPerformanceCalculable(record)) {
                    continue;
                }

                Double performanceTatHours =
                        record.getPerformanceTatHours();

                BigDecimal tatHours =
                        BigDecimal.valueOf(performanceTatHours)
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP
                                );

                BigDecimal actualHours =
                        calculateActualHours(
                                record.getStartedDate(),
                                record.getCompletedDate()
                        );

                BigDecimal performance =
                        calculatePerformancePercentage(
                                tatHours,
                                actualHours
                        );

                projectPerformanceTotal =
                        projectPerformanceTotal.add(performance);

                totalPerformance =
                        totalPerformance.add(performance);

                projectPerformanceCalculatedMilestones++;
                totalPerformanceCalculatedMilestones++;

                int comparison =
                        actualHours.compareTo(tatHours);

                if (comparison < 0) {

                    beforeTatCount++;
                    totalBeforeTat++;

                } else if (comparison == 0) {

                    withinTatCount++;
                    totalWithinTat++;

                } else {

                    delayedCount++;
                    totalDelayed++;
                }
            }

            BigDecimal projectAveragePerformance =
                    projectPerformanceCalculatedMilestones == 0
                            ? BigDecimal.ZERO.setScale(
                            2,
                            RoundingMode.HALF_UP
                    )
                            : projectPerformanceTotal.divide(
                            BigDecimal.valueOf(
                                    projectPerformanceCalculatedMilestones
                            ),
                            2,
                            RoundingMode.HALF_UP
                    );

            projectDetails.add(
                    UserProjectPerformanceDetailDto.builder()
                            .projectId(first.getProjectId())
                            .projectNumber(first.getProjectNumber())
                            .projectName(first.getProjectName())
                            .productId(first.getProductId())
                            .productName(first.getProductName())
                            .totalAssignedMilestones(
                                    (long) projectRecords.size()
                            )
                            .completedMilestones(
                                    projectCompletedMilestones
                            )
                            .beforeTatCount(beforeTatCount)
                            .withinTatCount(withinTatCount)
                            .delayedCount(delayedCount)
                            .performancePercentage(
                                    projectAveragePerformance
                            )
                            .build()
            );
        }

        BigDecimal averagePerformance =
                totalPerformanceCalculatedMilestones == 0
                        ? BigDecimal.ZERO.setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                        : totalPerformance.divide(
                        BigDecimal.valueOf(
                                totalPerformanceCalculatedMilestones
                        ),
                        2,
                        RoundingMode.HALF_UP
                );

        return UserProjectPerformanceResponseDto.builder()
                .userId(user.getId())
                .userName(user.getFullName())
                .totalProjects((long) projectGroups.size())
                .totalCompletedMilestones(
                        totalCompletedMilestones
                )
                .completedBeforeTat(totalBeforeTat)
                .completedWithinTat(totalWithinTat)
                .delayedMilestones(totalDelayed)
                .averagePerformancePercentage(
                        averagePerformance
                )
                .projectPerformance(projectDetails)
                .build();
    }


    private boolean isCompletedMilestone(
            UserMilestonePerformanceProjection record
    ) {

        return record.getStatusId() != null
                && record.getStatusId().equals(3L);
    }
    private boolean isPerformanceCalculable(
            UserMilestonePerformanceProjection record
    ) {

        if (!isCompletedMilestone(record)) {
            return false;
        }

        if (!Boolean.TRUE.equals(
                record.getPerformanceTatApplicable()
        )) {
            return false;
        }

        Double performanceTatHours =
                record.getPerformanceTatHours();

        if (performanceTatHours == null
                || performanceTatHours <= 0) {
            return false;
        }

        return record.getStartedDate() != null
                && record.getCompletedDate() != null;
    }

    private BigDecimal calculateActualHours(
            LocalDateTime startedDate,
            LocalDateTime completedDate
    ) {

        if (startedDate == null || completedDate == null) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        if (completedDate.isBefore(startedDate)) {
            throw new ValidationException(
                    "Completed date cannot be before started date",
                    "ERR_INVALID_COMPLETION_DATE"
            );
        }

        long totalMinutes = Duration.between(
                startedDate,
                completedDate
        ).toMinutes();

        return BigDecimal.valueOf(totalMinutes)
                .divide(
                        BigDecimal.valueOf(60),
                        2,
                        RoundingMode.HALF_UP
                );
    }
    private void validateRequest(
            Long userId,
            Long departmentId,
            int page,
            int size
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID is required"
            );
        }

        if (departmentId == null) {
            throw new IllegalArgumentException(
                    "Department ID is required"
            );
        }

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }
    }

    private void validateDepartmentAccess(
            Long userId,
            Long departmentId
    ) {

        Long count =
                userRepository.countActiveUserInDepartment(
                        userId,
                        departmentId
                );

        boolean hasAccess =
                count != null && count > 0;

        if (!hasAccess) {
            throw new ResourceNotFoundException(
                    "User does not have access to the selected department",
                    "ERR_USER_DEPARTMENT_ACCESS_DENIED"
            );
        }
    }

    private Map<Long, List<ProjectMilestoneTrackerProjection>>
    fetchMilestoneMap(List<Long> projectIds, Long departmentId) {

        List<ProjectMilestoneTrackerProjection> milestones =
                milestoneAssignmentRepository
                        .findTrackerMilestones(projectIds);

        return milestones.stream()
                .collect(
                        Collectors.groupingBy(
                                ProjectMilestoneTrackerProjection::getProjectId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                );
    }

    private Map<Long, Long> fetchPendingDocumentMap(
            List<Long> projectIds
    ) {

        List<ProjectPendingDocumentProjection> records =
                projectDocumentUploadRepository
                        .findPendingDocumentCounts(projectIds);

        return records.stream()
                .collect(
                        Collectors.toMap(
                                ProjectPendingDocumentProjection::getProjectId,
                                record -> safeLong(
                                        record.getPendingDocuments()
                                ),
                                (first, second) -> first,
                                HashMap::new
                        )
                );
    }

    private ProjectMilestoneTrackerResponseDto buildResponse(
            ProjectTrackerSummaryProjection project,
            List<ProjectMilestoneTrackerProjection>
                    milestoneRecords,
            Long pendingDocumentCount
    ) {

        List<ProjectMilestoneProgressDto> milestoneDtos =
                milestoneRecords.stream()
                        .sorted(
                                Comparator.comparing(
                                        record ->
                                                safeInteger(
                                                        record.getDisplayOrder()
                                                )
                                )
                        )
                        .map(this::buildMilestoneDto)
                        .toList();

        int overallPercentage =
                calculateOverallPercentage(milestoneDtos);

        ProjectMilestoneTrackerProjection currentMilestone =
                findCurrentMilestone(milestoneRecords);

        ProjectMilestoneTrackerProjection ownerRecord =
                findOwnerRecord(
                        currentMilestone,
                        milestoneRecords
                );

        LocalDate dueDate =
                currentMilestone != null
                        && currentMilestone.getDueDate() != null
                        ? currentMilestone.getDueDate()
                        : project.getDueDate();

        return ProjectMilestoneTrackerResponseDto.builder()
                .projectId(project.getProjectId())
                .projectNumber(project.getProjectNumber())
                .projectValue(project.getProjectValue())
                .companyId(project.getCompanyId())
                .companyName(project.getCompanyName())
                .productId(project.getProductId())
                .serviceName(project.getServiceName())
                .stageId(project.getStageId())
                .stage(project.getStage())
                .overallPercentage(overallPercentage)
                .currentMilestoneId(
                        currentMilestone == null
                                ? null
                                : currentMilestone.getMilestoneId()
                )
                .currentMilestoneName(
                        currentMilestone == null
                                ? null
                                : currentMilestone.getMilestoneName()
                )
                .pendingDocumentCount(
                        safeLong(pendingDocumentCount)
                )
                .dueDate(dueDate)
                .priority(
                        parsePriority(project.getPriority())
                )
                .ownerId(
                        ownerRecord == null
                                ? null
                                : ownerRecord.getAssignedUserId()
                )
                .ownerName(
                        ownerRecord == null
                                ? null
                                : ownerRecord.getAssignedUserName()
                )
                .milestones(milestoneDtos)
                .build();
    }

    private ProjectMilestoneProgressDto buildMilestoneDto(
            ProjectMilestoneTrackerProjection record
    ) {

        int percentage =
                normalizePercentage(
                        record.getProgressPercentage()
                );

        return ProjectMilestoneProgressDto.builder()
                .milestoneId(record.getMilestoneId())
                .milestoneName(record.getMilestoneName())
                .displayOrder(
                        safeInteger(record.getDisplayOrder())
                )
                .percentage(percentage)
                .statusId(record.getStatusId())
                .statusName(record.getStatusName())
                .completed(percentage >= 100)
                .assignedUserId(
                        record.getAssignedUserId()
                )
                .assignedUserName(
                        record.getAssignedUserName()
                )
                .build();
    }

    private int calculateOverallPercentage(
            List<ProjectMilestoneProgressDto> milestones
    ) {

        if (milestones == null || milestones.isEmpty()) {
            return 0;
        }

        double average =
                milestones.stream()
                        .map(
                                ProjectMilestoneProgressDto::getPercentage
                        )
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);

        return (int) Math.round(average);
    }

    private ProjectMilestoneTrackerProjection findCurrentMilestone(
            List<ProjectMilestoneTrackerProjection> records
    ) {

        if (records == null || records.isEmpty()) {
            return null;
        }

        return records.stream()
                .filter(record ->
                        normalizePercentage(
                                record.getProgressPercentage()
                        ) < 100
                )
                .sorted(
                        Comparator.comparing(
                                record ->
                                        safeInteger(
                                                record.getDisplayOrder()
                                        )
                        )
                )
                .findFirst()
                .orElseGet(() ->
                        records.stream()
                                .max(
                                        Comparator.comparing(
                                                record ->
                                                        safeInteger(
                                                                record.getDisplayOrder()
                                                        )
                                        )
                                )
                                .orElse(null)
                );
    }

    private ProjectMilestoneTrackerProjection findOwnerRecord(
            ProjectMilestoneTrackerProjection currentMilestone,
            List<ProjectMilestoneTrackerProjection> records
    ) {

        if (currentMilestone != null
                && currentMilestone.getAssignedUserId() != null) {
            return currentMilestone;
        }

        return records.stream()
                .filter(record ->
                        record.getAssignedUserId() != null
                )
                .findFirst()
                .orElse(null);
    }

    private ProjectPriority parsePriority(
            String priority
    ) {

        if (priority == null || priority.isBlank()) {
            return ProjectPriority.STANDARD;
        }

        try {
            return ProjectPriority.valueOf(
                    priority.trim().toUpperCase()
            );
        } catch (IllegalArgumentException exception) {



            return ProjectPriority.STANDARD;
        }
    }

    private int normalizePercentage(
            Integer percentage
    ) {

        if (percentage == null) {
            return 0;
        }

        return Math.max(
                0,
                Math.min(percentage, 100)
        );
    }

    private Integer safeInteger(Integer value) {
        return value == null
                ? Integer.MAX_VALUE
                : value;
    }

    private Long safeLong(Long value) {
        return value == null
                ? 0L
                : value;
    }
    private BigDecimal calculatePerformancePercentage(
            BigDecimal tatHours,
            BigDecimal actualHours
    ) {

        if (tatHours == null ||
                tatHours.compareTo(BigDecimal.ZERO) <= 0) {

            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal performance = BigDecimal.valueOf(100)
                .add(
                        tatHours.subtract(actualHours)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(
                                        tatHours,
                                        2,
                                        RoundingMode.HALF_UP
                                )
                );

        if (performance.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        return performance.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }



    private UserDepartmentProjection validateUserAndGetDepartment(
            Long userId
    ) {

        if (userId == null) {
            throw new ValidationException(
                    "User ID is required",
                    "USER_ID_REQUIRED"
            );
        }

        userRepository.findActiveUserById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Active user not found with ID: " + userId,
                                "USER_NOT_FOUND"
                        )
                );

        return userRepository
                .findActiveUserDepartment(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No department is mapped with user ID: " + userId,
                                "USER_DEPARTMENT_NOT_FOUND"
                        )
                );
    }
    private Long validateUserAndGetDepartmentId(Long userId) {

        if (userId == null) {
            throw new ValidationException(
                    "User ID is required",
                    "USER_ID_REQUIRED"
            );
        }

        User user = userRepository.findActiveUserById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Active user not found with ID: " + userId,
                                "ACTIVE_USER_NOT_FOUND"
                        )
                );

        boolean isAdmin = user.getRoles()
                .stream()
                .anyMatch(role ->
                        "ADMIN".equalsIgnoreCase(role.getName())
                );

        // Admin can see all department data
        if (isAdmin) {
            return null;
        }

        List<UserDepartmentProjection> departments =
                userRepository.findActiveDepartmentsByUserId(userId);

        if (departments == null || departments.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No department is mapped with user ID: " + userId,
                    "USER_DEPARTMENT_NOT_FOUND"
            );
        }

        if (departments.size() > 1) {
            throw new ValidationException(
                    "User is mapped with multiple departments",
                    "MULTIPLE_USER_DEPARTMENTS"
            );
        }

        return departments.get(0).getDepartmentId();
    }
}