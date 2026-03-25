package com.doc.controller.project;


import com.doc.dto.project.report.ProjectActivitySummaryDto;
import com.doc.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operationService/api/projects/report")
public class ProjectReport {

    @Autowired
    private ActivityService activityService;

    @GetMapping("/summary")
    public ResponseEntity<?> getProjectSummary(@RequestParam Long projectId) {

        try {
            ProjectActivitySummaryDto response =
                    activityService.getProjectSummary(projectId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
