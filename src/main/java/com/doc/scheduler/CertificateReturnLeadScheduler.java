package com.doc.scheduler;

import com.doc.dto.lead.ScheduledCertificateReturnLeadRequestDto;
import com.doc.dto.lead.ScheduledCertificationLeadResponseDto;
import com.doc.entity.client.Contact;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.feign.LeadFeignClient;
import com.doc.repository.ProjectMilestoneAssignmentRepository;

import feign.FeignException;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.Date;
import java.util.List;


@Component
@RequiredArgsConstructor
public class CertificateReturnLeadScheduler {


    // =========================================================
    // LOGGER
    // =========================================================

    private static final Logger logger =
            LoggerFactory.getLogger(
                    CertificateReturnLeadScheduler.class
            );


    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final ZoneId INDIA_ZONE =
            ZoneId.of(
                    "Asia/Kolkata"
            );


    /**
     * Return Lead will be picked when the
     * return expiry date is within next 10 days.
     */
    private static final long RETURN_LOOKAHEAD_DAYS =
            10L;


    private static final String RETURN_SOURCE =
            "CERTIFICATE_RETURN";


    private static final String TARGET_WORK_FUNCTION =
            "RD";


    private static final String SOURCE_REFERENCE_TYPE =
            "PROJECT_MILESTONE_ASSIGNMENT";


    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ProjectMilestoneAssignmentRepository
            projectMilestoneAssignmentRepository;


    private final LeadFeignClient
            leadFeignClient;


    // =========================================================
    // SCHEDULER
    // =========================================================

    /**
     * Runs every night at 1:30 AM.
     *
     * Operation identifies when Return is due.
     *
     * Lead Service identifies the RD user
     * mapped with the Solution.
     */
    @Scheduled(
            cron = "0 30 1 * * *"
    )
    public void checkCertificateReturnsAndCreateLeads() {


        LocalDate thresholdDate =
                LocalDate
                        .now(
                                INDIA_ZONE
                        )
                        .plusDays(
                                RETURN_LOOKAHEAD_DAYS
                        );


        logger.info(
                "[SCHEDULED-RETURN-LEAD-START] thresholdDate={}",
                thresholdDate
        );


        List<ProjectMilestoneAssignment> candidates =
                projectMilestoneAssignmentRepository
                        .findCertificateReturnsDueForLeadCreation(
                                thresholdDate
                        );


        if (candidates == null
                || candidates.isEmpty()) {


            logger.info(
                    "[SCHEDULED-RETURN-LEAD-NO-CANDIDATES] thresholdDate={}",
                    thresholdDate
            );


            return;
        }


        logger.info(
                "[SCHEDULED-RETURN-LEAD-CANDIDATES] count={}",
                candidates.size()
        );


        int successCount =
                0;


        int failureCount =
                0;


        // =====================================================
        // PROCESS RECORDS ONE BY ONE
        // =====================================================

        for (ProjectMilestoneAssignment assignment :
                candidates) {


            try {


                boolean created =
                        processReturn(
                                assignment
                        );


                if (created) {


                    successCount++;


                } else {


                    failureCount++;
                }


            } catch (Exception exception) {


                failureCount++;


                logger.error(
                        "[SCHEDULED-RETURN-LEAD-PROCESSING-ERROR] " +
                                "assignmentId={}, error={}",

                        assignment != null
                                ? assignment.getId()
                                : null,

                        exception.getMessage(),

                        exception
                );
            }
        }


        logger.info(
                "[SCHEDULED-RETURN-LEAD-COMPLETE] " +
                        "total={}, success={}, failed={}",

                candidates.size(),

                successCount,

                failureCount
        );
    }


    // =========================================================
    // PROCESS SINGLE RETURN
    // =========================================================

    protected boolean processReturn(
            ProjectMilestoneAssignment assignment
    ) {


        // =====================================================
        // 1. ASSIGNMENT
        // =====================================================

        if (assignment == null) {


            logger.warn(
                    "[SCHEDULED-RETURN-LEAD-SKIPPED] " +
                            "reason=ASSIGNMENT_NULL"
            );


            return false;
        }


        // =====================================================
        // 2. ALREADY CREATED
        // =====================================================

        if (assignment.isReturnLeadCreated()) {


            logger.info(
                    "[SCHEDULED-RETURN-LEAD-SKIPPED] " +
                            "assignmentId={}, leadId={}, " +
                            "reason=ALREADY_CREATED",

                    assignment.getId(),

                    assignment.getReturnLeadId()
            );


            return true;
        }


        // =====================================================
        // 3. RETURN DATE
        // =====================================================

        if (assignment
                .getReturnTenureExpirationDate() == null) {


            logger.warn(
                    "[SCHEDULED-RETURN-LEAD-SKIPPED] " +
                            "assignmentId={}, " +
                            "reason=RETURN_EXPIRATION_DATE_MISSING",

                    assignment.getId()
            );


            return false;
        }


        // =====================================================
        // 4. PROJECT
        // =====================================================

        Project project =
                assignment.getProject();


        if (project == null
                || project.getId() == null) {


            markScheduledReturnFailure(
                    assignment,
                    "Project information is missing"
            );


            return false;
        }


        // =====================================================
        // 5. SOLUTION
        // =====================================================

        if (project.getProduct() == null
                || project
                .getProduct()
                .getId() == null) {


            markScheduledReturnFailure(
                    assignment,
                    "Project Product/Solution information is missing"
            );


            return false;
        }


        // =====================================================
        // 6. COMPANY
        // =====================================================

        if (project.getCompany() == null
                || project
                .getCompany()
                .getId() == null) {


            markScheduledReturnFailure(
                    assignment,
                    "Project Company information is missing"
            );


            return false;
        }


        // =====================================================
        // 7. REGISTER ATTEMPT
        // =====================================================

        Integer currentAttemptCount =
                assignment
                        .getReturnLeadAttemptCount();


        assignment.setReturnLeadAttemptCount(

                currentAttemptCount == null
                        ? 1
                        : currentAttemptCount + 1
        );


        assignment.setReturnLeadLastAttemptAt(
                LocalDateTime.now(
                        INDIA_ZONE
                )
        );


        assignment.setReturnLeadError(
                null
        );


        assignment.setUpdatedDate(
                new Date()
        );


        projectMilestoneAssignmentRepository.save(
                assignment
        );


        // =====================================================
        // 8. BUILD REQUEST
        // =====================================================

        ScheduledCertificateReturnLeadRequestDto request =
                buildScheduledReturnRequest(
                        assignment,
                        project
                );


        // =====================================================
        // 9. CALL LEAD SERVICE
        // =====================================================

        try {


            logger.info(
                    "[SCHEDULED-RETURN-LEAD-REQUEST] " +
                            "assignmentId={}, " +
                            "projectId={}, " +
                            "projectNo={}, " +
                            "originalLeadId={}, " +
                            "solutionId={}, " +
                            "companyId={}, " +
                            "returnTenure={}, " +
                            "returnExpirationDate={}, " +
                            "targetWorkFunction={}",

                    assignment.getId(),

                    request.getProjectId(),

                    request.getProjectNo(),

                    request.getOriginalLeadId(),

                    request.getSolutionId(),

                    request.getCompanyId(),

                    request.getReturnTenure(),

                    request.getReturnTenureExpirationDate(),

                    request.getTargetWorkFunction()
            );


            ScheduledCertificationLeadResponseDto response =
                    leadFeignClient
                            .createScheduledCertificateReturnLead(
                                    request
                            );


            // =================================================
            // 10. EMPTY RESPONSE
            // =================================================

            if (response == null) {


                markScheduledReturnFailure(
                        assignment,
                        "Lead Service returned empty response"
                );


                return false;
            }


            // =================================================
            // 11. INVALID LEAD
            // =================================================

            if (response.getLeadId() == null
                    || response.getLeadId() <= 0) {


                markScheduledReturnFailure(
                        assignment,
                        "Lead Service returned invalid Lead ID"
                );


                return false;
            }


            // =================================================
            // 12. SUCCESS
            // =================================================

            assignment.setReturnLeadCreated(
                    true
            );


            assignment.setReturnLeadId(
                    response.getLeadId()
            );


            assignment.setReturnLeadCreatedAt(
                    LocalDateTime.now(
                            INDIA_ZONE
                    )
            );


            assignment.setReturnLeadLastAttemptAt(
                    LocalDateTime.now(
                            INDIA_ZONE
                    )
            );


            assignment.setReturnLeadError(
                    null
            );


            assignment.setUpdatedDate(
                    new Date()
            );


            projectMilestoneAssignmentRepository.save(
                    assignment
            );


            logger.info(
                    "[SCHEDULED-RETURN-LEAD-SUCCESS] " +
                            "assignmentId={}, " +
                            "projectId={}, " +
                            "leadId={}, " +
                            "solutionId={}, " +
                            "assignedUserId={}, " +
                            "assignedUserName={}, " +
                            "workFunction={}, " +
                            "existingLead={}",

                    assignment.getId(),

                    project.getId(),

                    response.getLeadId(),

                    response.getSolutionId(),

                    response.getAssignedUserId(),

                    response.getAssignedUserName(),

                    response.getAssignmentWorkFunction(),

                    response.isExistingLead()
            );


            return true;


        } catch (FeignException exception) {


            logger.error(
                    "[SCHEDULED-RETURN-LEAD-FEIGN-ERROR] " +
                            "assignmentId={}, projectId={}, " +
                            "httpStatus={}, error={}",

                    assignment.getId(),

                    project.getId(),

                    exception.status(),

                    exception.getMessage(),

                    exception
            );


            markScheduledReturnFailure(
                    assignment,

                    "Lead Service HTTP "
                            + exception.status()
                            + ": "
                            + exception.getMessage()
            );


            return false;


        } catch (Exception exception) {


            logger.error(
                    "[SCHEDULED-RETURN-LEAD-ERROR] " +
                            "assignmentId={}, projectId={}, error={}",

                    assignment.getId(),

                    project.getId(),

                    exception.getMessage(),

                    exception
            );


            markScheduledReturnFailure(
                    assignment,
                    exception.getMessage()
            );


            return false;
        }
    }


    // =========================================================
    // BUILD RETURN REQUEST
    // =========================================================

    private ScheduledCertificateReturnLeadRequestDto
    buildScheduledReturnRequest(
            ProjectMilestoneAssignment assignment,
            Project project
    ) {


        Contact contact =
                project.getContact();


        return ScheduledCertificateReturnLeadRequestDto
                .builder()


                // =================================================
                // PROJECT
                // =================================================

                .projectId(
                        project.getId()
                )


                .projectNo(
                        project.getProjectNo()
                )


                .originalLeadId(
                        project.getLeadId()
                )


                // =================================================
                // SOLUTION
                //
                // Operation Product ID =
                // Lead Service Solution ID
                // =================================================

                .solutionId(
                        project
                                .getProduct()
                                .getId()
                )


                // =================================================
                // COMPANY
                // =================================================

                .companyId(
                        project
                                .getCompany()
                                .getId()
                )


                .companyName(
                        project
                                .getCompany()
                                .getName()
                )


                // =================================================
                // CONTACT
                // =================================================

                .contactId(
                        contact != null
                                ? contact.getId()
                                : null
                )


                .contactName(
                        contact != null
                                ? safeTrim(
                                contact.getName()
                        )
                                : null
                )


                .contactEmail(
                        contact != null
                                ? safeTrim(
                                contact.getEmail()
                        )
                                : null
                )


                .contactMobile(
                        contact != null
                                ? safeTrim(
                                contact.getContactNo()
                        )
                                : null
                )


                // =================================================
                // OPERATION REFERENCE
                // =================================================

                .milestoneAssignmentId(
                        assignment.getId()
                )


                .sourceReferenceType(
                        SOURCE_REFERENCE_TYPE
                )


                .sourceReferenceId(
                        assignment.getId()
                )


                // =================================================
                // CERTIFICATE RETURN
                // =================================================

                .certificateIssueDate(
                        assignment
                                .getCertificateIssueDate()
                )


                .returnTenure(
                        assignment
                                .getReturnTenure()
                )


                .returnTenureExpirationDate(
                        assignment
                                .getReturnTenureExpirationDate()
                )


                // =================================================
                // ROUTING
                // =================================================

                .source(
                        RETURN_SOURCE
                )


                .targetWorkFunction(
                        TARGET_WORK_FUNCTION
                )


                .build();
    }


    // =========================================================
    // MARK FAILURE
    // =========================================================

    private void markScheduledReturnFailure(
            ProjectMilestoneAssignment assignment,
            String errorMessage
    ) {


        if (assignment == null) {


            return;
        }


        String error =
                safeTrim(
                        errorMessage
                );


        if (error == null) {


            error =
                    "Unknown scheduled Return Lead creation error";
        }


        if (error.length() > 1000) {


            error =
                    error.substring(
                            0,
                            1000
                    );
        }


        assignment.setReturnLeadCreated(
                false
        );


        assignment.setReturnLeadError(
                error
        );


        assignment.setReturnLeadLastAttemptAt(
                LocalDateTime.now(
                        INDIA_ZONE
                )
        );


        assignment.setUpdatedDate(
                new Date()
        );


        projectMilestoneAssignmentRepository.save(
                assignment
        );


        logger.error(
                "[SCHEDULED-RETURN-LEAD-FAILED] " +
                        "assignmentId={}, error={}",

                assignment.getId(),

                error
        );
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safeTrim(
            String value
    ) {


        if (value == null) {


            return null;
        }


        String normalized =
                value.trim();


        if (normalized.isEmpty()
                || "NA".equalsIgnoreCase(
                normalized
        )) {


            return null;
        }


        return normalized;
    }
}