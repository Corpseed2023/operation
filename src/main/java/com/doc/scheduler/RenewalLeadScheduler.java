package com.doc.scheduler;

import com.doc.dto.LeadDTO;
import com.doc.entity.client.Company;
import com.doc.entity.client.Contact;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.feign.LeadFeignClient;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RenewalLeadScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RenewalLeadScheduler.class);

    private static final long RENEWAL_LOOKAHEAD_DAYS = 40L;


    private final ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;

    private final LeadFeignClient leadFeignClient;

    /**
     * Runs every night at 1:00 AM.
     * Finds every active certificate whose renewalDueDate is within the next
     * 40 days (or already past) and for which a lead hasn't been created yet,
     * then creates a lead via the Lead Service for each.
     */
//    @Scheduled(cron = "0 0 1 * * *")
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkRenewalsAndCreateLeads() {

        LocalDate thresholdDate = LocalDate.now().plusDays(RENEWAL_LOOKAHEAD_DAYS);

        logger.info("[RENEWAL-LEAD-SCHEDULER-START] thresholdDate={}", thresholdDate);

        List<ProjectMilestoneAssignment> candidates =
                projectMilestoneAssignmentRepository.findRenewalsDueForLeadCreation(thresholdDate);

        logger.info("[RENEWAL-LEAD-SCHEDULER-CANDIDATES] count={}", candidates.size());

        int successCount = 0;
        int failureCount = 0;

        for (ProjectMilestoneAssignment assignment : candidates) {
            try {
                boolean created = processRenewal(assignment);
                if (created) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                // One bad record must never stop the rest of the batch.
                failureCount++;
                logger.error(
                        "[RENEWAL-LEAD-PROCESSING-ERROR] assignmentId={}, error={}",
                        assignment.getId(), e.getMessage(), e
                );
            }
        }

        logger.info(
                "[RENEWAL-LEAD-SCHEDULER-COMPLETE] total={}, created={}, failed={}",
                candidates.size(), successCount, failureCount
        );
    }

    protected boolean processRenewal(ProjectMilestoneAssignment assignment) {
        if (assignment.getRenewalDueDate() == null) {
            logger.warn("[RENEWAL-LEAD-SKIPPED] assignmentId={} has no renewalDueDate", assignment.getId());
            return false;
        }

        LeadDTO leadDTO = buildLeadDto(assignment);
        boolean success = callCreateLead(leadDTO);

        if (success) {
            assignment.setRenewalLeadCreated(true);
            assignment.setUpdatedDate(new Date());
            projectMilestoneAssignmentRepository.save(assignment);

            logger.info(
                    "[RENEWAL-LEAD-FLAGGED] assignmentId={}, renewalDueDate={}",
                    assignment.getId(), assignment.getRenewalDueDate()
            );
        } else {
            logger.warn(
                    "[RENEWAL-LEAD-RETRY-NEXT-RUN] assignmentId={} will be retried on next scheduled run",
                    assignment.getId()
            );
        }

        return success;
    }

    /**
     * Calls the Lead Service directly via Feign.
     * Returns true only on a genuine 2xx/CREATED response — false on any
     * 4xx/5xx or network failure, so the caller skips marking the renewal
     * as processed and it gets retried on the next scheduled run.
     */
    private boolean callCreateLead(LeadDTO leadDTO) {
        try {
            ResponseEntity<Object> response = leadFeignClient.createLead(leadDTO);

            if (response != null
                    && (response.getStatusCode() == HttpStatus.CREATED
                    || response.getStatusCode().is2xxSuccessful())) {

                logger.info(
                        "[RENEWAL-LEAD-CREATED] leadName={}, productId={}, status={}",
                        leadDTO.getLeadName(), leadDTO.getProductId(), response.getStatusCode()
                );
                return true;
            }

            logger.warn(
                    "[RENEWAL-LEAD-UNEXPECTED-STATUS] leadName={}, status={}",
                    leadDTO.getLeadName(), response != null ? response.getStatusCode() : "null response"
            );
            return false;

        } catch (FeignException e) {
            logger.error(
                    "[RENEWAL-LEAD-CALL-FAILED] leadName={}, productId={}, status={}, error={}",
                    leadDTO.getLeadName(), leadDTO.getProductId(), e.status(), e.getMessage(), e
            );
            return false;

        } catch (Exception e) {
            logger.error(
                    "[RENEWAL-LEAD-CALL-UNEXPECTED-ERROR] leadName={}, productId={}, error={}",
                    leadDTO.getLeadName(), leadDTO.getProductId(), e.getMessage(), e
            );
            return false;
        }
    }

    /**
     * Maps a ProjectMilestoneAssignment (renewal-eligible certificate) to a LeadDTO.
     * Adjust field sourcing below once you confirm what your Project entity
     * exposes for client name/email/mobile (needed for validateContactInfo
     * on the Lead Service side).
     */
    private LeadDTO buildLeadDto(ProjectMilestoneAssignment assignment) {
        Project project = assignment.getProject();

        LeadDTO dto = new LeadDTO();

        dto.setName(project != null ? safeProjectName(project) : "Renewal Lead");
        dto.setLeadName(project != null ? safeProjectName(project) : "Renewal Lead");
        dto.setLeadDescription(
                "Auto-generated renewal lead. Certificate expiring on "
                        + assignment.getCertificateExpiryDate()
                        + " (renewal due " + assignment.getRenewalDueDate() + ")"
        );

        dto.setSource("SYSTEM_RENEWAL");
        dto.setProductId(project != null && project.getProduct() != null
                ? project.getProduct().getId() : null);

        // Resolve contact info: prefer the project's direct contact (unit-level person),
        // fall back to company-level contact if the project has no contact assigned.
        resolveContactInfo(dto, project);

        dto.setAuto(true);
        dto.setCreateDate(new Date());
        dto.setLastUpdated(new Date());
        dto.setDeleted(false);

        return dto;
    }

    /**
     * Populates email/mobile/name on the LeadDTO from the project's Contact,
     * falling back to Company-level fields if no direct contact is set.
     *
     * NOTE: Adjust getter names below (getMobile, getPhone, etc.) to match
     * your actual Contact/Company entity field names if they differ.
     */
    private void resolveContactInfo(LeadDTO dto, Project project) {
        if (project == null) {
            logger.warn("[RENEWAL-LEAD-NO-PROJECT] Cannot resolve contact info, project is null");
            return;
        }

        Contact contact = project.getContact();

        if (contact != null) {
            dto.setEmail(safeTrim(contact.getEmail()));
            dto.setMobileNo(safeTrim(contact.getContactNo()));

            if (dto.getName() == null || dto.getName().isBlank()) {
                dto.setName(safeTrim(contact.getName()));
            }

            logger.debug(
                    "[RENEWAL-LEAD-CONTACT-RESOLVED] projectId={}, contactId={}, hasEmail={}, hasMobile={}",
                    project.getId(), contact.getId(),
                    dto.getEmail() != null, dto.getMobileNo() != null
            );
        }

        // Fallback to company-level contact info if the direct contact
        // didn't give us an email or mobile
        if ((dto.getEmail() == null || dto.getEmail().isBlank())
                && (dto.getMobileNo() == null || dto.getMobileNo().isBlank())) {

            Company company = project.getCompany();

            if (company != null) {
                dto.setEmail(safeTrim(contact.getEmail()));
                dto.setMobileNo(safeTrim(contact.getContactNo()));

                logger.debug(
                        "[RENEWAL-LEAD-CONTACT-FALLBACK-COMPANY] projectId={}, companyId={}, hasEmail={}, hasMobile={}",
                        project.getId(), company.getId(),
                        dto.getEmail() != null, dto.getMobileNo() != null
                );
            }
        }

        if ((dto.getEmail() == null || dto.getEmail().isBlank())
                && (dto.getMobileNo() == null || dto.getMobileNo().isBlank())) {

            logger.warn(
                    "[RENEWAL-LEAD-MISSING-CONTACT] projectId={} has no email or mobile on contact or company — lead creation will fail validation",
                    project.getId()
            );
        }
    }

    private String safeTrim(String value) {

        return value != null && !value.isBlank() ? value.trim() : null ;
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