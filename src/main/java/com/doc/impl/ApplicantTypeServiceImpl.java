package com.doc.impl;

import com.doc.dto.document.ApplicantTypeRequestDto;
import com.doc.dto.document.ApplicantTypeResponseDto;
import com.doc.entity.document.ApplicantType;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.documentRepo.ApplicantTypeRepository;
import com.doc.service.ApplicantTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApplicantTypeServiceImpl implements ApplicantTypeService {

    @Autowired
    private ApplicantTypeRepository applicantTypeRepository;

    @Override
    public ApplicantTypeResponseDto createApplicantType(ApplicantTypeRequestDto dto) {
        String trimmedName = dto.getName().trim();

        // Step 1: If a soft-deleted record with same name exists, restore it
        Optional<ApplicantType> softDeletedOpt = applicantTypeRepository
                .findByNameIgnoreCaseAndIsDeletedTrue(trimmedName);

        if (softDeletedOpt.isPresent()) {
            ApplicantType existing = softDeletedOpt.get();
            existing.setDeleted(false);
            existing.setActive(true);
            existing.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : existing.getDescription());

            ApplicantType saved = applicantTypeRepository.save(existing);
            return mapToResponseDto(saved);
        }

        // Step 2: Check for active duplicate
        if (applicantTypeRepository.existsByNameIgnoreCaseAndIsDeletedFalse(trimmedName)) {
            throw new ValidationException("Applicant type with name '" + trimmedName + "' already exists",
                    "ERR_DUPLICATE_APPLICANT_TYPE");
        }

        // Step 3: Create new record
        ApplicantType applicantType = new ApplicantType();
        applicantType.setName(trimmedName);
        applicantType.setDescription(dto.getDescription());
        applicantType.setActive(true);
        applicantType.setDeleted(false);

        try {
            applicantType = applicantTypeRepository.save(applicantType);
        } catch (DataIntegrityViolationException ex) {
            // Fallback in case of rare race condition
            throw new ValidationException("Applicant type with name '" + trimmedName + "' already exists",
                    "ERR_DUPLICATE_APPLICANT_TYPE");
        }

        return mapToResponseDto(applicantType);
    }

    @Override
    public ApplicantTypeResponseDto updateApplicantType(Long id, ApplicantTypeRequestDto dto) {
        ApplicantType entity = applicantTypeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant type not found", "ERR_APPLICANT_TYPE_NOT_FOUND"));

        String trimmedName = dto.getName().trim();

        if (applicantTypeRepository.existsByNameIgnoreCaseAndIsDeletedFalseAndIdNot(trimmedName, id)) {
            throw new ValidationException("Another applicant type with name '" + trimmedName + "' already exists",
                    "ERR_DUPLICATE_APPLICANT_TYPE");
        }

        entity.setName(trimmedName);
        entity.setDescription(dto.getDescription());
        entity = applicantTypeRepository.save(entity);

        return mapToResponseDto(entity);
    }

    @Override
    public ApplicantTypeResponseDto getApplicantTypeById(Long id) {
        ApplicantType entity = applicantTypeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant type not found", "ERR_APPLICANT_TYPE_NOT_FOUND"));
        return mapToResponseDto(entity);
    }

    @Override
    public List<ApplicantTypeResponseDto> getApplicantTypesPaginated(int page, int size) {
        page = Math.max(page, 1);
        size = size > 0 ? size : 10;

        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by("name").ascending());

        return applicantTypeRepository.findByIsDeletedFalse(pageRequest)
                .getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private ApplicantTypeResponseDto mapToResponseDto(ApplicantType entity) {
        ApplicantTypeResponseDto dto = new ApplicantTypeResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.isActive());
        return dto;
    }

    @Override
    public void softDeleteApplicantType(Long id) {
        ApplicantType entity = applicantTypeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant type not found", "ERR_APPLICANT_TYPE_NOT_FOUND"));

        entity.setDeleted(true);
        entity.setActive(false);

        applicantTypeRepository.save(entity);
    }
}