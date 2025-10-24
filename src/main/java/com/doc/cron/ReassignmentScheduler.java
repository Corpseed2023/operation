package com.doc.cron;

import com.doc.entity.project.AssignmentResult;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.user.User;
import com.doc.entity.user.UserLoginStatus;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.repository.UserLoginStatusRepository;
import com.doc.service.AutoAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Scheduler for reassigning milestones when assigned users are unavailable for more than 1 day.
 */
@Component
@Transactional
public class ReassignmentScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReassignmentScheduler.class);

    @Autowired
    private ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;

    @Autowired
    private UserLoginStatusRepository userLoginStatusRepository;

    @Autowired
    private AutoAssignmentService autoAssignmentService;

    /**
     * Runs hourly to reassign milestones for users offline >1 day.
     */
    @Scheduled(fixedRate = 3600000) // Hourly
    public void reassignOfflineUsers() {
        logger.info("Starting reassignment scheduler for offline users");

        LocalDate today = LocalDate.now();
        List<ProjectMilestoneAssignment> assignments = projectMilestoneAssignmentRepository
                .findByStatusInAndIsDeletedFalse(List.of("NEW", "IN_PROGRESS"));
        logger.debug("Found {} assignments to check for reassignment", assignments.size());

        for (ProjectMilestoneAssignment assignment : assignments) {
            User user = assignment.getAssignedUser();
            if (user == null) {
                logger.debug("Assignment ID {} has no assigned user, skipping", assignment.getId());
                continue;
            }

            UserLoginStatus status = userLoginStatusRepository.findByUserIdAndIsDeletedFalse(user.getId()).orElse(null);
            if (status == null || status.isOnline() || status.getLastOnline() == null) {
                logger.debug("User ID {} has no valid login status or is online, skipping", user.getId());
                continue;
            }

            LocalDate lastOnlineDate = status.getLastOnline().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastOnlineDate.isBefore(today.minusDays(1))) {
                logger.info("User ID {} offline >1 day, reassigning milestone ID {}", user.getId(), assignment.getId());

                // Try reassigning to another executive
                AssignmentResult result = autoAssignmentService.assignMilestoneUser(
                        assignment.getProductMilestoneMap(), assignment.getUpdatedBy());

                if (result.getUser() != null) {
                    // Successfully reassigned
                    assignment.setAssignedUser(result.getUser());
                    assignment.setStatusReason("Reassigned due to original user offline >1 day");
                    projectMilestoneAssignmentRepository.save(assignment);
                    logger.info("Reassigned milestone ID {} to user ID {}: {}", assignment.getId(), result.getUser().getId(), result.getReason());

                    // Notify client (placeholder)
                    logger.info("Notifying client of reassignment for milestone ID {}", assignment.getId());
                    // Example: notificationService.notifyClient(assignment, result.getUser());
                } else {
                    // No alternative executive; assign to manager
                    User manager = user.getManager();
                    if (manager != null && !manager.isDeleted() && manager.isActive()) {
                        assignment.setAssignedUser(manager);
                        assignment.setStatusReason("Reassigned to manager due to no available executives");
                        projectMilestoneAssignmentRepository.save(assignment);
                        logger.info("Reassigned milestone ID {} to manager ID {}", assignment.getId(), manager.getId());

                        // Notify client (placeholder)
                        logger.info("Notifying client of manager reassignment for milestone ID {}", assignment.getId());
                        // Example: notificationService.notifyClient(assignment, manager);
                    } else {
                        // Queue if no manager available
                        assignment.setStatusReason("Queued due to no available executives or manager");
                        // Set QUEUED status (assumes exists in milestone_statuses)
                        projectMilestoneAssignmentRepository.save(assignment);
                        logger.warn("No manager available for milestone ID {}, queued", assignment.getId());

                        // Notify Operation Head (placeholder)
                        logger.info("Notifying Operation Head for queued milestone ID {}", assignment.getId());
                        // Example: notificationService.notifyOperationHead(assignment);
                    }
                }
            }
        }
        logger.info("Reassignment scheduler completed");
    }
}