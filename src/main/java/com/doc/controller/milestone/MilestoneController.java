package com.doc.controller.milestone;

import com.doc.dto.milestone.MilestoneRequestDto;
import com.doc.dto.milestone.MilestoneResponseDto;
import com.doc.service.MilestoneService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing Milestone entities.
 */
@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    @Autowired
    private MilestoneService milestoneService;

    /**
     * Creates a new milestone and associates it with departments.
     *
     * @param requestDto the milestone data to create
     * @return the created milestone
     */
    @PostMapping
    public ResponseEntity<MilestoneResponseDto> createMilestone(
            @Valid @RequestBody MilestoneRequestDto requestDto) {
        MilestoneResponseDto response = milestoneService.createMilestone(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Updates an existing milestone and its department associations.
     *
     * @param id the milestone ID
     * @param requestDto the updated milestone data
     * @return the updated milestone
     */
    @PutMapping("/{id}")
    public ResponseEntity<MilestoneResponseDto> updateMilestone(
            @PathVariable Long id,
            @Valid @RequestBody MilestoneRequestDto requestDto) {
        MilestoneResponseDto response = milestoneService.updateMilestone(id, requestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    /**
     * Retrieves a milestone by ID.
     *
     * @param id the milestone ID
     * @return the milestone if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<MilestoneResponseDto> getMilestoneById(@PathVariable Long id) {
        MilestoneResponseDto response = milestoneService.getMilestoneById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieves all milestones with pagination.
     *
     * @param pageable pagination information
     * @return a page of milestones
     */
    @GetMapping
    public ResponseEntity<Page<MilestoneResponseDto>> getAllMilestones(Pageable pageable) {
        Page<MilestoneResponseDto> milestones = milestoneService.getAllMilestones(pageable);
        return new ResponseEntity<>(milestones, HttpStatus.OK);
    }

    /**
     * Deletes a milestone by ID (soft delete).
     *
     * @param id the milestone ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable Long id) {
        milestoneService.deleteMilestone(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieves milestones by department ID with pagination.
     *
     * @param departmentId the department ID
     * @param pageable pagination information
     * @return a page of milestones associated with the department
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Page<MilestoneResponseDto>> getMilestonesByDepartment(
            @PathVariable Long departmentId, Pageable pageable) {
        Page<MilestoneResponseDto> milestones = milestoneService.getMilestonesByDepartment(departmentId, pageable);
        return new ResponseEntity<>(milestones, HttpStatus.OK);
    }


    /**
     * Retrieves all milestones without pagination.
     *
     * @return list of all milestones
     */
    @GetMapping("/all")
    public ResponseEntity<List<MilestoneResponseDto>> getAllMilestonesList() {
        List<MilestoneResponseDto> milestones = milestoneService.getAllMilestones();
        return new ResponseEntity<>(milestones, HttpStatus.OK);
    }





}