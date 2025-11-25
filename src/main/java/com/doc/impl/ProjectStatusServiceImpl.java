package com.doc.impl;

import com.doc.dto.project.status.ProjectStatusRequestDto;
import com.doc.dto.project.status.ProjectStatusResponseDto;
import com.doc.entity.project.ProjectStatus;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.service.ProjectStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectStatusServiceImpl implements ProjectStatusService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectStatusServiceImpl.class);

    @Autowired
    private ProjectStatusRepository statusRepository;

    @Override
    public ProjectStatusResponseDto createStatus(ProjectStatusRequestDto requestDto) {
        logger.info("Creating new project status: {}", requestDto.getName());

        if (statusRepository.existsByNameIgnoreCase(requestDto.getName().trim())) {
            throw new ValidationException("Status name '" + requestDto.getName() + "' already exists", "ERR_DUPLICATE_STATUS_NAME");
        }

        ProjectStatus status = new ProjectStatus();
        status.setName(requestDto.getName().trim().toUpperCase());
        status.setDescription(requestDto.getDescription());

        status = statusRepository.save(status);
        logger.info("Project status created with ID: {}", status.getId());

        return mapToResponseDto(status);
    }

    @Override
    public ProjectStatusResponseDto updateStatus(Long id, ProjectStatusRequestDto requestDto) {
        logger.info("Updating project status ID: {}", id);

        ProjectStatus status = statusRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project status not found", "ERR_STATUS_NOT_FOUND"));

        if (statusRepository.existsByNameIgnoreCaseAndIdNot(requestDto.getName().trim(), id)) {
            throw new ValidationException("Status name '" + requestDto.getName() + "' already exists", "ERR_DUPLICATE_STATUS_NAME");
        }

        status.setName(requestDto.getName().trim().toUpperCase());
        status.setDescription(requestDto.getDescription());

        status = statusRepository.save(status);
        logger.info("Project status updated ID: {}", id);

        return mapToResponseDto(status);
    }

    @Override
    public List<ProjectStatusResponseDto> getAllStatuses() {
        logger.info("Fetching all project statuses");
        return statusRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectStatusResponseDto getStatusById(Long id) {
        logger.info("Fetching project status by ID: {}", id);
        ProjectStatus status = statusRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project status not found", "ERR_STATUS_NOT_FOUND"));
        return mapToResponseDto(status);
    }

    @Override
    public void deleteStatus(Long id) {
        logger.info("Soft deleting project status ID: {}", id);
        ProjectStatus status = statusRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project status not found", "ERR_STATUS_NOT_FOUND"));

        statusRepository.delete(status);
        logger.info("Project status deleted ID: {}", id);
    }

    private ProjectStatusResponseDto mapToResponseDto(ProjectStatus status) {
        ProjectStatusResponseDto dto = new ProjectStatusResponseDto();
        dto.setId(status.getId());
        dto.setName(status.getName());
        dto.setDescription(status.getDescription());
        return dto;
    }
}