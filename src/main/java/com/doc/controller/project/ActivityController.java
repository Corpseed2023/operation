package com.doc.controller.project;



import com.doc.dto.document.DocumentChecklistDTO;
import com.doc.dto.project.AssignedProjectResponseDto;
import com.doc.dto.project.ProjectMilestoneResponseDto;
import com.doc.dto.project.ProjectRequestDto;
import com.doc.dto.project.ProjectResponseDto;
import com.doc.dto.project.projectHistory.MilestoneHistoryResponseDto;
import com.doc.dto.project.projectHistory.ProjectHistoryResponseDto;
import com.doc.dto.transaction.ProjectPaymentTransactionDto;
import com.doc.service.ProjectSearchService;
import com.doc.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/activity")
public class ActivityController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectSearchService projectSearchService;



}
