package com.doc.scheduler;

import com.doc.dto.LeadDTO;
import com.doc.entity.client.Contact;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.feign.LeadFeignClient;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * Creates a "Certificate Return" Lead 10 days before the
 * returnTenureExpirationDate of a completed Certification milestone.
 *
 * Mirrors RenewalLeadScheduler, but works off:
 *   certificateReturn = 'YES'
 *   returnTenureExpirationDate <= today + 10 days
 *   returnLeadCreated = false
 */
@Component
@RequiredArgsConstructor
public class CertificateReturnLeadScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(CertificateReturnLeadScheduler.class);

    private static final long RETURN_LOOKAHEAD_DAYS = 10L;

    private final ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;

    private final LeadFeignClient leadFeignClient;

    /**
     * Runs every night at 1:30 AM (staggered from the renewal scheduler at 1:00 AM).
     */
    @Scheduled(cron = "0 30 1 * * *")
    @Transactional
    public void checkCertificateReturnsAndCreateLeads() {

        LocalDate thresholdDate = LocalDate.now().plusDays(RETURN_LOOKAHEAD_DAYS);

        logger.info("[RETURN-LEAD-SCHEDULER-START] thresholdDate={}", thresholdDate);

        List<ProjectMilestoneAssignment> candidates =
                projectMilestoneAssignmentRepository
                        .findCertificateReturnsDueForLeadCreation(thresholdDate);

        logger.info("[RETURN-LEAD-SCHEDULER-CANDIDATES] count={}", candidates.size());

        int successCount = 0;
        int failureCount = 0;

        for (ProjectMilestoneAssignment assignment : candidates) {
            try {
                if (processReturn(assignment)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                // One bad record must never stop the rest of the batch.
                failureCount++;
                logger.error(
                        "[RETURN-LEAD-PROCESSING-ERROR] assignmentId={}, error={}",
                        assignment.getId(), e.getMessage(), e
                );
            }
        }

        logger.info(
                "[RETURN-LEAD-SCHEDULER-COMPLETE] total={}, created={}, failed={}",
                candidates.size(), successCount, failureCount
        );
    }

    protected boolean processReturn(ProjectMilestoneAssignment assignment) {

        if (assignment.getReturnTenureExpirationDate() == null) {
            logger.warn(
                    "[RETURN-LEAD-SKIPPED] assignmentId={} has no returnTenureExpirationDate",
                    assignment.getId()
            );
            return false;
        }

        LeadDTO leadDTO = buildLeadDto(assignment);
        boolean success = callCreateLead(leadDTO);

        if (success) {
            assignment.setReturnLeadCreated(true);
            assignment.setReturnLeadCreatedAt(LocalDateTime.now());
            assignment.setUpdatedDate(new Date());
            projectMilestoneAssignmentRepository.save(assignment);

            logger.info(
                    "[RETURN-LEAD-FLAGGED] assignmentId={}, returnTenureExpirationDate={}",
                    assignment.getId(), assignment.getReturnTenureExpirationDate()
            );
        } else {
            logger.warn(
                    "[RETURN-LEAD-RETRY-NEXT-RUN] assignmentId={} will be retried on next scheduled run",
                    assignment.getId()
            );
        }

        return success;
    }

    /**
     * Returns true only on a genuine 2xx response. Any 4xx/5xx or network
     * failure returns false so the record is retried on the next run.
     */
    private boolean callCreateLead(LeadDTO leadDTO) {
        try {
            ResponseEntity<Object> response = leadFeignClient.createLead(leadDTO);

            if (response != null
                    && (response.getStatusCode() == HttpStatus.CREATED
                    || response.getStatusCode().is2xxSuccessful())) {

                logger.info(
                        "[RETURN-LEAD-CREATED] leadName={}, productId={}, status={}",
                        leadDTO.getLeadName(), leadDTO.getProductId(), response.getStatusCode()
                );
                return true;
            }

            logger.warn(
                    "[RETURN-LEAD-UNEXPECTED-STATUS] leadName={}, status={}",
                    leadDTO.getLeadName(),
                    response != null ? response.getStatusCode() : "null response"
            );
            return false;

        } catch (FeignException e) {
            logger.error(
                    "[RETURN-LEAD-CALL-FAILED] leadName={}, productId={}, status={}, error={}",
                    leadDTO.getLeadName(), leadDTO.getProductId(), e.status(), e.getMessage(), e
            );
            return false;

        } catch (Exception e) {
            logger.error(
                    "[RETURN-LEAD-CALL-UNEXPECTED-ERROR] leadName={}, productId={}, error={}",
                    leadDTO.getLeadName(), leadDTO.getProductId(), e.getMessage(), e
            );
            return false;
        }
    }

    private LeadDTO buildLeadDto(ProjectMilestoneAssignment assignment) {
        Project project = assignment.getProject();

        String projectName = project != null ? safeProjectName(project) : "Project";

        LeadDTO dto = new LeadDTO();

        dto.setName(projectName);
        dto.setLeadName("Certificate Return - " + projectName);
        dto.setLeadDescription(
                "Auto-generated certificate return lead. Return tenure ("
                        + assignment.getReturnTenure()
                        + ") expires on "
                        + assignment.getReturnTenureExpirationDate()
        );

        dto.setSource("SYSTEM_CERTIFICATE_RETURN");
        dto.setProductId(project != null && project.getProduct() != null
                ? project.getProduct().getId() : null);

        resolveContactInfo(dto, project);

        dto.setAuto(true);
        dto.setCreateDate(new Date());
        dto.setLastUpdated(new Date());
        dto.setDeleted(false);

        return dto;
    }

    private void resolveContactInfo(LeadDTO dto, Project project) {
        if (project == null) {
            logger.warn("[RETURN-LEAD-NO-PROJECT] Cannot resolve contact info, project is null");
            return;
        }

        Contact contact = project.getContact();

        if (contact != null) {
            dto.setEmail(safeTrim(contact.getEmail()));
            dto.setMobileNo(safeTrim(contact.getContactNo()));

            if (dto.getName() == null || dto.getName().isBlank()) {
                dto.setName(safeTrim(contact.getName()));
            }
        }

        if ((dto.getEmail() == null || dto.getEmail().isBlank())
                && (dto.getMobileNo() == null || dto.getMobileNo().isBlank())) {

            logger.warn(
                    "[RETURN-LEAD-MISSING-CONTACT] projectId={} has no email or mobile on the project contact — lead creation may fail validation",
                    project.getId()
            );
        }
    }

    private String safeTrim(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private String safeProjectName(Project project) {
        try {
            return project.getName() != null && !project.getName().isBlank()
                    ? project.getName().trim()
                    : "Project-" + project.getId();
        } catch (Exception e) {
            return "Project-" + project.getId();
        }
    }
}