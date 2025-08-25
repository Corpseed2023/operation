package com.doc.impl;

import com.doc.dto.milestone.MilestoneRequestDto;
import com.doc.dto.milestone.MilestoneResponseDto;
import com.doc.entity.product.Milestone;
import com.doc.entity.user.Department;
import com.doc.repository.DepartmentRepository;
import com.doc.repository.MilestoneRepository;
import com.doc.service.MilestoneService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing Milestone entities.
 */
@Service
@Transactional
public class MilestoneServiceImpl implements MilestoneService {

    private static final Logger logger = LoggerFactory.getLogger(MilestoneServiceImpl.class);

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public MilestoneResponseDto createMilestone(MilestoneRequestDto requestDto) {
        logger.info("Creating milestone with name: {}", requestDto.getName());
        if (milestoneRepository.existsByName(requestDto.getName())) {
            logger.error("Milestone with name {} already exists", requestDto.getName());
            throw new IllegalArgumentException("Milestone with name " + requestDto.getName() + " already exists");
        }

        Milestone milestone = new Milestone();
        milestone.setName(requestDto.getName().trim());
        milestone.setDescription(requestDto.getDescription());

        // Set departments if provided
        List<Department> departments = new ArrayList<>();
        if (requestDto.getDepartmentIds() != null && !requestDto.getDepartmentIds().isEmpty()) {
            logger.info("Associating milestone with department IDs: {}", requestDto.getDepartmentIds());
            departments = requestDto.getDepartmentIds().stream()
                    .map(id -> departmentRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + id)))
                    .filter(dept -> !dept.isDeleted())
                    .collect(Collectors.toList());
            milestone.setDepartments(departments);

            // Update the Department side of the relationship
            for (Department dept : departments) {
                List<Milestone> deptMilestones = dept.getMilestones();
                if (deptMilestones == null) {
                    deptMilestones = new ArrayList<>();
                    dept.setMilestones(deptMilestones);
                }
                if (!deptMilestones.contains(milestone)) {
                    deptMilestones.add(milestone);
                }
            }
        } else {
            milestone.setDepartments(departments);
        }

        Milestone savedMilestone = milestoneRepository.save(milestone);
        logger.info("Milestone created with ID: {}", savedMilestone.getId());
        return mapToResponseDto(savedMilestone);
    }

    @Override
    public MilestoneResponseDto updateMilestone(Long id, MilestoneRequestDto requestDto) {
        logger.info("Updating milestone with ID: {}", id);
        Milestone existingMilestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Milestone not found with ID: " + id));

        // Check for name conflict with other milestones
        if (!existingMilestone.getName().equals(requestDto.getName()) &&
                milestoneRepository.existsByName(requestDto.getName())) {
            logger.error("Milestone with name {} already exists", requestDto.getName());
            throw new IllegalArgumentException("Milestone with name " + requestDto.getName() + " already exists");
        }

        // Clear existing department associations on the Department side
        List<Department> oldDepartments = existingMilestone.getDepartments();
        if (oldDepartments != null) {
            for (Department dept : oldDepartments) {
                dept.getMilestones().remove(existingMilestone);
            }
        }

        existingMilestone.setName(requestDto.getName().trim());

        // Update departments if provided
        List<Department> departments = new ArrayList<>();
        if (requestDto.getDepartmentIds() != null && !requestDto.getDepartmentIds().isEmpty()) {
            logger.info("Updating milestone with department IDs: {}", requestDto.getDepartmentIds());
            departments = requestDto.getDepartmentIds().stream()
                    .map(deptId -> departmentRepository.findById(deptId)
                            .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + deptId)))
                    .filter(dept -> !dept.isDeleted())
                    .collect(Collectors.toList());
            existingMilestone.setDepartments(departments);

            // Update the Department side of the relationship
            for (Department dept : departments) {
                List<Milestone> deptMilestones = dept.getMilestones();
                if (deptMilestones == null) {
                    deptMilestones = new ArrayList<>();
                    dept.setMilestones(deptMilestones);
                }
                if (!deptMilestones.contains(existingMilestone)) {
                    deptMilestones.add(existingMilestone);
                }
            }
        } else {
            existingMilestone.setDepartments(departments);
        }

        Milestone updatedMilestone = milestoneRepository.save(existingMilestone);
        logger.info("Milestone updated with ID: {}", updatedMilestone.getId());
        return mapToResponseDto(updatedMilestone);
    }

    @Override
    public MilestoneResponseDto getMilestoneById(Long id) {
        logger.info("Fetching milestone with ID: {}", id);
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Milestone not found with ID: " + id));
        return mapToResponseDto(milestone);
    }

    @Override
    public void deleteMilestone(Long id) {
        logger.info("Deleting milestone with ID: {}", id);
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Milestone not found with ID: " + id));

        // Clear department associations on the Department side
        List<Department> departments = milestone.getDepartments();
        if (departments != null) {
            for (Department dept : departments) {
                dept.getMilestones().remove(milestone);
            }
        }

        milestoneRepository.delete(milestone);
        logger.info("Milestone deleted with ID: {}", id);
    }

    @Override
    public Page<MilestoneResponseDto> getMilestonesByDepartment(Long departmentId, Pageable pageable) {
        logger.info("Fetching milestones for department ID: {} with pageable: {}", departmentId, pageable);
        if (!departmentRepository.existsByIdAndIsDeletedFalse(departmentId)) {
            logger.error("Department not found with ID: {}", departmentId);
            throw new EntityNotFoundException("Department not found with ID: " + departmentId);
        }
        return milestoneRepository.findByDepartmentsId(departmentId, pageable)
                .map(this::mapToResponseDto);
    }

    private MilestoneResponseDto mapToResponseDto(Milestone milestone) {
        MilestoneResponseDto dto = new MilestoneResponseDto();
        dto.setId(milestone.getId());
        dto.setName(milestone.getName());
        dto.setDescription(milestone.getDescription());
        dto.setDepartmentIds(milestone.getDepartments().stream()
                .map(Department::getId)
                .collect(Collectors.toList()));
        return dto;
    }


    @Override
    public List<MilestoneResponseDto> getAllMilestones() {

        List<Milestone> milestones = milestoneRepository.findAll();

        return milestones.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
}