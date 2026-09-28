package com.doc.scheduler;

import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.service.project.CertificationRenewalLeadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
public class CertificationRenewalLeadScheduler {

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");


    private final ProjectMilestoneAssignmentRepository
            projectMilestoneAssignmentRepository;

    private final CertificationRenewalLeadService
            certificationRenewalLeadService;


    public CertificationRenewalLeadScheduler(
            ProjectMilestoneAssignmentRepository
                    projectMilestoneAssignmentRepository,
            CertificationRenewalLeadService
                    certificationRenewalLeadService
    ) {

        this.projectMilestoneAssignmentRepository =
                projectMilestoneAssignmentRepository;

        this.certificationRenewalLeadService =
                certificationRenewalLeadService;
    }


    @Scheduled(
            cron =
                    "${scheduler.certification-renewal-lead.cron:0 0 6 * * *}",
            zone = "Asia/Kolkata"
    )
    public void processCertificationRenewalLeads() {

        LocalDate today =
                LocalDate.now(
                        INDIA_ZONE
                );


        log.info(
                "[CERTIFICATION-RENEWAL-SCHEDULER-START] date={}",
                today
        );


        List<Long> assignmentIds =
                projectMilestoneAssignmentRepository
                        .findPendingCertificationRenewalLeadAssignmentIds(
                                today
                        );


        if (assignmentIds == null
                || assignmentIds.isEmpty()) {

            log.info(
                    "[CERTIFICATION-RENEWAL-SCHEDULER-END] " +
                            "date={} | eligibleAssignments=0",
                    today
            );

            return;
        }


        log.info(
                "[CERTIFICATION-RENEWAL-SCHEDULER-FOUND] " +
                        "date={} | eligibleAssignments={}",
                today,
                assignmentIds.size()
        );


        int successCount = 0;
        int errorCount = 0;


        for (Long assignmentId : assignmentIds) {

            if (assignmentId == null) {
                continue;
            }


            try {

                certificationRenewalLeadService
                        .processRenewalLead(
                                assignmentId
                        );


                successCount++;


            } catch (Exception exception) {

                errorCount++;


                /*
                 * One failed Project must never stop
                 * processing the remaining renewals.
                 */
                log.error(
                        "[CERTIFICATION-RENEWAL-SCHEDULER-ITEM-ERROR] " +
                                "assignmentId={} | error={}",
                        assignmentId,
                        exception.getMessage(),
                        exception
                );
            }
        }


        log.info(
                "[CERTIFICATION-RENEWAL-SCHEDULER-END] " +
                        "date={} | eligibleAssignments={} | " +
                        "processed={} | unexpectedErrors={}",
                today,
                assignmentIds.size(),
                successCount,
                errorCount
        );
    }
}