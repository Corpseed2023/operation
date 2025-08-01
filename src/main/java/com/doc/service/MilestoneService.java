package com.doc.service;

import com.doc.dto.milestone.MilestoneRequestDto;
import com.doc.dto.milestone.MilestoneResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing Milestone entities.
 */
public interface MilestoneService {

    /**
     * Creates a new milestone and associates it with departments.
     *
     * @param requestDto the milestone data to create
     * @return the created milestone
     */
    MilestoneResponseDto createMilestone(MilestoneRequestDto requestDto);

    /**
     * Updates an existing milestone and its department associations.
     *
     * @param id the milestone ID
     * @param requestDto the updated milestone data
     * @return the updated milestone
     */
    MilestoneResponseDto updateMilestone(Long id, MilestoneRequestDto requestDto);

    /**
     * Retrieves a milestone by ID.
     *
     * @param id the milestone ID
     * @return the milestone
     */
    MilestoneResponseDto getMilestoneById(Long id);

    /**
     * Retrieves all milestones with pagination.
     *
     * @param pageable pagination information
     * @return a page of milestones
     */
    Page<MilestoneResponseDto> getAllMilestones(Pageable pageable);

    /**
     * Deletes a milestone by ID (soft delete).
     *
     * @param id the milestone ID
     */
    void deleteMilestone(Long id);

    /**
     * Retrieves milestones by department ID with pagination.
     *
     * @param departmentId the department ID
     * @param pageable pagination information
     * @return a page of milestones
     */
    Page<MilestoneResponseDto> getMilestonesByDepartment(Long departmentId, Pageable pageable);
}