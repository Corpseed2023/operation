package com.doc.scheduler;

import com.doc.dto.lead.ScheduledCertificationLeadResponseDto;
import com.doc.dto.lead.ScheduledCertificationRenewalLeadRequestDto;
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
public class RenewalLeadScheduler {


    // =========================================================
    // LOGGER
    // =========================================================

    private static final Logger logger =
            LoggerFactory.getLogger(
                    RenewalLeadScheduler.class
            );


    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final ZoneId INDIA_ZONE =
            ZoneId.of(
                    "Asia/Kolkata"
            );


    /**
     * Scheduler will fetch certificates whose
     * renewalDueDate is within next 40 days.
     */
    private static final long RENEWAL_LOOKAHEAD_DAYS =
            40L;


    /**
     * Lead source sent to Lead Service.
     */
    private static final String RENEWAL_SOURCE =
            "CERTIFICATE_RENEWAL";


    /**
     * This scheduler always routes the Lead
     * to Renewal Desk.
     *
     * Lead Service will actually identify
     * the RD user mapped with the Solution.
     */
    private static final String TARGET_WORK_FUNCTION =
            "RD";


    /**
     * Operation-side source reference.
     */
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
     * Runs every night at 1:00 AM.
     *
     * Operation Service:
     *
     * 1. Finds Renewal records.
     * 2. Sends Project/Solution/Company information.
     *
     * Lead Service:
     *
     * 1. Finds Solution mapping.
     * 2. Finds RD users.
     * 3. Selects eligible RD.
     * 4. Creates Lead directly for RD.
     */
    @Scheduled(
            cron = "0 0 1 * * *"
    )
    public void checkRenewalsAndCreateLeads() {


        LocalDate thresholdDate =
                LocalDate
                        .now(
                                INDIA_ZONE
                        )
                        .plusDays(
                                RENEWAL_LOOKAHEAD_DAYS
                        );


        logger.info(
                "[SCHEDULED-RENEWAL-LEAD-START] thresholdDate={}",
                thresholdDate
        );


        List<ProjectMilestoneAssignment> candidates =
                projectMilestoneAssignmentRepository
                        .findRenewalsDueForLeadCreation(
                                thresholdDate
                        );


        if (candidates == null
                || candidates.isEmpty()) {


            logger.info(
                    "[SCHEDULED-RENEWAL-LEAD-NO-CANDIDATES] thresholdDate={}",
                    thresholdDate
            );


            return;
        }


        logger.info(
                "[SCHEDULED-RENEWAL-LEAD-CANDIDATES] count={}",
                candidates.size()
        );


        int successCount =
                0;


        int failureCount =
                0;


        // =====================================================
        // PROCESS EACH RENEWAL SEPARATELY
        //
        // One failure must never stop remaining records.
        // =====================================================

        for (ProjectMilestoneAssignment assignment :
                candidates) {


            try {


                boolean created =
                        processRenewal(
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
                        "[SCHEDULED-RENEWAL-LEAD-PROCESSING-ERROR] " +
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
                "[SCHEDULED-RENEWAL-LEAD-COMPLETE] " +
                        "total={}, success={}, failed={}",

                candidates.size(),

                successCount,

                failureCount
        );
    }


    // =========================================================
    // PROCESS SINGLE RENEWAL
    // =========================================================

    protected boolean processRenewal(
            ProjectMilestoneAssignment assignment
    ) {


        // =====================================================
        // 1. VALIDATE ASSIGNMENT
        // =====================================================

        if (assignment == null) {


            logger.warn(
                    "[SCHEDULED-RENEWAL-LEAD-SKIPPED] " +
                            "reason=ASSIGNMENT_NULL"
            );


            return false;
        }


        // =====================================================
        // 2. DUPLICATE PROTECTION
        // =====================================================

        if (assignment.isRenewalLeadCreated()) {


            logger.info(
                    "[SCHEDULED-RENEWAL-LEAD-SKIPPED] " +
                            "assignmentId={}, leadId={}, " +
                            "reason=ALREADY_CREATED",

                    assignment.getId(),

                    assignment.getRenewalLeadId()
            );


            return true;
        }


        // =====================================================
        // 3. RENEWAL DATE
        // =====================================================

        if (assignment.getRenewalDueDate() == null) {


            logger.warn(
                    "[SCHEDULED-RENEWAL-LEAD-SKIPPED] " +
                            "assignmentId={}, " +
                            "reason=RENEWAL_DUE_DATE_MISSING",

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


            markScheduledRenewalFailure(
                    assignment,
                    "Project information is missing"
            );


            return false;
        }


        // =====================================================
        // 5. PRODUCT / SOLUTION
        // =====================================================

        if (project.getProduct() == null
                || project
                .getProduct()
                .getId() == null) {


            markScheduledRenewalFailure(
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


            markScheduledRenewalFailure(
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
                        .getRenewalLeadAttemptCount();


        assignment.setRenewalLeadAttemptCount(

                currentAttemptCount == null
                        ? 1
                        : currentAttemptCount + 1
        );


        assignment.setRenewalLeadLastAttemptAt(
                LocalDateTime.now(
                        INDIA_ZONE
                )
        );


        assignment.setRenewalLeadError(
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

        ScheduledCertificationRenewalLeadRequestDto request =
                buildScheduledRenewalRequest(
                        assignment,
                        project
                );


        // =====================================================
        // 9. CALL NEW LEAD SERVICE API
        // =====================================================

        try {


            logger.info(
                    "[SCHEDULED-RENEWAL-LEAD-REQUEST] " +
                            "assignmentId={}, " +
                            "projectId={}, " +
                            "projectNo={}, " +
                            "originalLeadId={}, " +
                            "solutionId={}, " +
                            "companyId={}, " +
                            "renewalDueDate={}, " +
                            "certificateExpiryDate={}, " +
                            "targetWorkFunction={}",

                    assignment.getId(),

                    request.getProjectId(),

                    request.getProjectNo(),

                    request.getOriginalLeadId(),

                    request.getSolutionId(),

                    request.getCompanyId(),

                    request.getRenewalDueDate(),

                    request.getCertificateExpiryDate(),

                    request.getTargetWorkFunction()
            );


            ScheduledCertificationLeadResponseDto response =
                    leadFeignClient
                            .createScheduledCertificationRenewalLead(
                                    request
                            );


            // =================================================
            // 10. EMPTY RESPONSE
            // =================================================

            if (response == null) {


                markScheduledRenewalFailure(
                        assignment,
                        "Lead Service returned empty response"
                );


                return false;
            }


            // =================================================
            // 11. VALIDATE LEAD ID
            // =================================================

            if (response.getLeadId() == null
                    || response.getLeadId() <= 0) {


                markScheduledRenewalFailure(
                        assignment,
                        "Lead Service returned invalid Lead ID"
                );


                return false;
            }


            // =================================================
            // 12. SUCCESS
            // =================================================

            assignment.setRenewalLeadCreated(
                    true
            );


            assignment.setRenewalLeadId(
                    response.getLeadId()
            );


            assignment.setRenewalLeadCreatedAt(
                    LocalDateTime.now(
                            INDIA_ZONE
                    )
            );


            assignment.setRenewalLeadLastAttemptAt(
                    LocalDateTime.now(
                            INDIA_ZONE
                    )
            );


            assignment.setRenewalLeadError(
                    null
            );


            assignment.setUpdatedDate(
                    new Date()
            );


            projectMilestoneAssignmentRepository.save(
                    assignment
            );


            logger.info(
                    "[SCHEDULED-RENEWAL-LEAD-SUCCESS] " +
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
                    "[SCHEDULED-RENEWAL-LEAD-FEIGN-ERROR] " +
                            "assignmentId={}, projectId={}, " +
                            "httpStatus={}, error={}",

                    assignment.getId(),

                    project.getId(),

                    exception.status(),

                    exception.getMessage(),

                    exception
            );


            markScheduledRenewalFailure(
                    assignment,

                    "Lead Service HTTP "
                            + exception.status()
                            + ": "
                            + exception.getMessage()
            );


            return false;


        } catch (Exception exception) {


            logger.error(
                    "[SCHEDULED-RENEWAL-LEAD-ERROR] " +
                            "assignmentId={}, projectId={}, error={}",

                    assignment.getId(),

                    project.getId(),

                    exception.getMessage(),

                    exception
            );


            markScheduledRenewalFailure(
                    assignment,
                    exception.getMessage()
            );


            return false;
        }
    }


    // =========================================================
    // BUILD RENEWAL REQUEST
    // =========================================================

    private ScheduledCertificationRenewalLeadRequestDto
    buildScheduledRenewalRequest(
            ProjectMilestoneAssignment assignment,
            Project project
    ) {


        Contact contact =
                project.getContact();


        return ScheduledCertificationRenewalLeadRequestDto
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
                // CERTIFICATE
                // =================================================

                .certificateIssueDate(
                        assignment
                                .getCertificateIssueDate()
                )


                .certificateExpiryDate(
                        assignment
                                .getCertificateExpiryDate()
                )


                .renewalDueDate(
                        assignment
                                .getRenewalDueDate()
                )


                // =================================================
                // LEAD ROUTING
                // =================================================

                .source(
                        RENEWAL_SOURCE
                )


                .targetWorkFunction(
                        TARGET_WORK_FUNCTION
                )


                .build();
    }


    // =========================================================
    // MARK FAILURE
    // =========================================================

    private void markScheduledRenewalFailure(
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
                    "Unknown scheduled Renewal Lead creation error";
        }


        if (error.length() > 1000) {


            error =
                    error.substring(
                            0,
                            1000
                    );
        }


        assignment.setRenewalLeadCreated(
                false
        );


        assignment.setRenewalLeadError(
                error
        );


        assignment.setRenewalLeadLastAttemptAt(
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
                "[SCHEDULED-RENEWAL-LEAD-FAILED] " +
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