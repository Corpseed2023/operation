package com.doc.impl.project;

import com.doc.dto.lead.CertificationRenewalLeadRequestDto;
import com.doc.dto.lead.CertificationRenewalLeadResponseDto;
import com.doc.entity.client.Contact;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.feign.LeadFeignClient;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.service.project.CertificationRenewalLeadService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
public class CertificationRenewalLeadServiceImpl
        implements CertificationRenewalLeadService {

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final String RENEWAL_SOURCE =
            "CERTIFICATE_RENEWAL";

    private static final String TARGET_WORK_FUNCTION =
            "RD";

    private static final String SOURCE_REFERENCE_TYPE =
            "PROJECT_MILESTONE_ASSIGNMENT";

    private final ProjectMilestoneAssignmentRepository
            projectMilestoneAssignmentRepository;

    private final LeadFeignClient
            leadFeignClient;


    public CertificationRenewalLeadServiceImpl(
            ProjectMilestoneAssignmentRepository
                    projectMilestoneAssignmentRepository,
            LeadFeignClient leadFeignClient
    ) {
        this.projectMilestoneAssignmentRepository =
                projectMilestoneAssignmentRepository;

        this.leadFeignClient =
                leadFeignClient;
    }


    @Override
    @Transactional
    public void processRenewalLead(
            Long milestoneAssignmentId
    ) {

        log.info(
                "[CERTIFICATION-RENEWAL-LEAD-START] assignmentId={}",
                milestoneAssignmentId
        );


        // =========================================================
        // 1. LOCK ASSIGNMENT
        // =========================================================

        ProjectMilestoneAssignment assignment =
                projectMilestoneAssignmentRepository
                        .findByIdForRenewalLeadProcessing(
                                milestoneAssignmentId
                        )
                        .orElse(null);


        if (assignment == null) {

            log.warn(
                    "[CERTIFICATION-RENEWAL-LEAD-SKIPPED] " +
                            "assignmentId={} | reason=ASSIGNMENT_NOT_FOUND",
                    milestoneAssignmentId
            );

            return;
        }


        // =========================================================
        // 2. DUPLICATE PROTECTION
        // =========================================================

        if (assignment.isRenewalLeadCreated()) {

            log.info(
                    "[CERTIFICATION-RENEWAL-LEAD-SKIPPED] " +
                            "assignmentId={} | reason=ALREADY_CREATED | leadId={}",
                    assignment.getId(),
                    assignment.getRenewalLeadId()
            );

            return;
        }


        // =========================================================
        // 3. STATUS VALIDATION
        // =========================================================

        if (assignment.getStatus() == null
                || assignment.getStatus().getName() == null
                || !"COMPLETED".equalsIgnoreCase(
                assignment.getStatus().getName()
        )) {

            log.info(
                    "[CERTIFICATION-RENEWAL-LEAD-SKIPPED] " +
                            "assignmentId={} | reason=MILESTONE_NOT_COMPLETED",
                    assignment.getId()
            );

            return;
        }


        // =========================================================
        // 4. CERTIFICATION MILESTONE VALIDATION
        // =========================================================

        if (assignment.getMilestone() == null
                || assignment.getMilestone().getName() == null
                || !"CERTIFICATION".equalsIgnoreCase(
                assignment.getMilestone()
                        .getName()
                        .trim()
        )) {

            log.info(
                    "[CERTIFICATION-RENEWAL-LEAD-SKIPPED] " +
                            "assignmentId={} | reason=NOT_CERTIFICATION_MILESTONE",
                    assignment.getId()
            );

            return;
        }


        // =========================================================
        // 5. CERTIFICATE VALIDATION
        // =========================================================

        if (assignment.getCertificateExpiryDate() == null) {

            log.info(
                    "[CERTIFICATION-RENEWAL-LEAD-SKIPPED] " +
                            "assignmentId={} | reason=NO_CERTIFICATE_EXPIRY",
                    assignment.getId()
            );

            return;
        }


        if (assignment.getRenewalDueDate() == null) {

            log.info(
                    "[CERTIFICATION-RENEWAL-LEAD-SKIPPED] " +
                            "assignmentId={} | reason=NO_RENEWAL_DUE_DATE",
                    assignment.getId()
            );

            return;
        }


        // =========================================================
        // 6. DATE VALIDATION
        // =========================================================

        LocalDate today =
                LocalDate.now(
                        INDIA_ZONE
                );


        if (assignment.getRenewalDueDate()
                .isAfter(today)) {

            log.info(
                    "[CERTIFICATION-RENEWAL-LEAD-SKIPPED] " +
                            "assignmentId={} | renewalDueDate={} | today={} | " +
                            "reason=NOT_DUE_YET",
                    assignment.getId(),
                    assignment.getRenewalDueDate(),
                    today
            );

            return;
        }


        // =========================================================
        // 7. PROJECT VALIDATION
        // =========================================================

        Project project =
                assignment.getProject();


        if (project == null
                || project.getId() == null) {

            markFailure(
                    assignment,
                    "Project information is missing"
            );

            return;
        }


        if (project.getProduct() == null
                || project.getProduct().getId() == null) {

            markFailure(
                    assignment,
                    "Project Product/Solution information is missing"
            );

            return;
        }


        if (project.getCompany() == null
                || project.getCompany().getId() == null) {

            markFailure(
                    assignment,
                    "Project Company information is missing"
            );

            return;
        }


        // =========================================================
        // 8. REGISTER ATTEMPT
        // =========================================================

        Integer currentAttemptCount =
                assignment.getRenewalLeadAttemptCount();


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


        projectMilestoneAssignmentRepository.save(
                assignment
        );


        // =========================================================
        // 9. BUILD REQUEST
        // =========================================================

        CertificationRenewalLeadRequestDto request =
                buildRequest(
                        assignment,
                        project
                );


        // =========================================================
        // 10. CALL LEAD SERVICE
        // =========================================================

        try {

            log.info(
                    "[CERTIFICATION-RENEWAL-LEAD-FEIGN-REQUEST] " +
                            "assignmentId={} | projectId={} | originalLeadId={} | " +
                            "solutionId={} | companyId={} | renewalDueDate={} | " +
                            "certificateExpiryDate={} | targetWorkFunction={}",
                    assignment.getId(),
                    project.getId(),
                    project.getLeadId(),
                    project.getProduct().getId(),
                    project.getCompany().getId(),
                    assignment.getRenewalDueDate(),
                    assignment.getCertificateExpiryDate(),
                    TARGET_WORK_FUNCTION
            );


            CertificationRenewalLeadResponseDto response =
                    leadFeignClient
                            .createCertificationRenewalLead(
                                    request
                            );


            // =====================================================
            // 11. VALIDATE RESPONSE
            // =====================================================

            if (response == null) {

                markFailure(
                        assignment,
                        "Lead Service returned empty response"
                );

                return;
            }


            if (response.getLeadId() == null
                    || response.getLeadId() <= 0) {

                markFailure(
                        assignment,
                        "Lead Service returned invalid Lead ID"
                );

                return;
            }


            // =====================================================
            // 12. SUCCESS
            // =====================================================

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


            projectMilestoneAssignmentRepository.save(
                    assignment
            );


            log.info(
                    "[CERTIFICATION-RENEWAL-LEAD-SUCCESS] " +
                            "assignmentId={} | projectId={} | renewalLeadId={} | " +
                            "assignedUserId={} | assignedUserName={} | " +
                            "workFunction={} | existingLead={}",
                    assignment.getId(),
                    project.getId(),
                    response.getLeadId(),
                    response.getAssignedUserId(),
                    response.getAssignedUserName(),
                    response.getAssignmentWorkFunction(),
                    response.isExistingLead()
            );


        } catch (FeignException exception) {

            log.error(
                    "[CERTIFICATION-RENEWAL-LEAD-FEIGN-ERROR] " +
                            "assignmentId={} | projectId={} | httpStatus={} | error={}",
                    assignment.getId(),
                    project.getId(),
                    exception.status(),
                    exception.getMessage(),
                    exception
            );


            markFailure(
                    assignment,
                    "Lead Service error. HTTP "
                            + exception.status()
                            + ": "
                            + exception.getMessage()
            );


        } catch (Exception exception) {

            log.error(
                    "[CERTIFICATION-RENEWAL-LEAD-ERROR] " +
                            "assignmentId={} | projectId={} | error={}",
                    assignment.getId(),
                    project.getId(),
                    exception.getMessage(),
                    exception
            );


            markFailure(
                    assignment,
                    exception.getMessage()
            );
        }
    }


    // =========================================================
    // BUILD LEAD REQUEST
    // =========================================================

    private CertificationRenewalLeadRequestDto buildRequest(
            ProjectMilestoneAssignment assignment,
            Project project
    ) {

        Contact contact =
                project.getContact();


        return CertificationRenewalLeadRequestDto
                .builder()

                .projectId(
                        project.getId()
                )

                .projectNo(
                        project.getProjectNo()
                )

                .originalLeadId(
                        project.getLeadId()
                )

                .solutionId(
                        project.getProduct().getId()
                )

                .companyId(
                        project.getCompany().getId()
                )

                .companyName(
                        project.getCompany().getName()
                )

                .contactId(
                        contact != null
                                ? contact.getId()
                                : null
                )

                .contactName(
                        contact != null
                                ? contact.getName()
                                : null
                )

                .contactEmail(
                        contact != null
                                ? contact.getEmail()
                                : null
                )

                .contactMobile(
                        contact != null
                                ? contact.getContactNo()
                                : null
                )

                .milestoneAssignmentId(
                        assignment.getId()
                )

                .sourceReferenceType(
                        SOURCE_REFERENCE_TYPE
                )

                .sourceReferenceId(
                        assignment.getId()
                )

                .certificateIssueDate(
                        assignment.getCertificateIssueDate()
                )

                .certificateExpiryDate(
                        assignment.getCertificateExpiryDate()
                )

                .renewalDueDate(
                        assignment.getRenewalDueDate()
                )

                .source(
                        RENEWAL_SOURCE
                )

                .targetWorkFunction(
                        TARGET_WORK_FUNCTION
                )

                .build();
    }


    // =========================================================
    // FAILURE
    // =========================================================

    private void markFailure(
            ProjectMilestoneAssignment assignment,
            String errorMessage
    ) {

        if (assignment == null) {
            return;
        }


        String error =
                errorMessage;


        if (error == null
                || error.trim().isEmpty()) {

            error =
                    "Unknown renewal Lead creation error";
        }


        error =
                error.trim();


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


        projectMilestoneAssignmentRepository.save(
                assignment
        );


        log.warn(
                "[CERTIFICATION-RENEWAL-LEAD-FAILED] " +
                        "assignmentId={} | projectId={} | attemptCount={} | error={}",
                assignment.getId(),
                assignment.getProject() != null
                        ? assignment.getProject().getId()
                        : null,
                assignment.getRenewalLeadAttemptCount(),
                error
        );
    }
}