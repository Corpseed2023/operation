// src/main/java/com/doc/impl/ApplicantTypeServiceImpl.java
package com.doc.impl;

import com.doc.dto.document.ApplicantTypeRequestDto;
import com.doc.dto.document.ApplicantTypeResponseDto;
import com.doc.entity.document.ApplicantType;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.documentRepo.ApplicantTypeRepository;
import com.doc.service.ApplicantTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApplicantTypeServiceImpl implements ApplicantTypeService {

    @Autowired
    private ApplicantTypeRepository applicantTypeRepository;

    @Override
    public ApplicantTypeResponseDto createApplicantType(ApplicantTypeRequestDto applicantTypeRequestDto) {
        if (applicantTypeRepository.existsByNameIgnoreCaseAndIsDeletedFalse(applicantTypeRequestDto.getName().trim())) {
            throw new ValidationException("Applicant type with name '" + applicantTypeRequestDto.getName() + "' already exists", "ERR_DUPLICATE_APPLICANT_TYPE");
        }

        ApplicantType applicantType = new ApplicantType();
        applicantType.setName(applicantTypeRequestDto.getName().trim());
        applicantType.setDescription(applicantTypeRequestDto.getDescription());
        applicantType.setActive(true);
        applicantType.setDeleted(false);

        applicantType = applicantTypeRepository.save(applicantType);
        return mapToResponseDto(applicantType);
    }

    @Override
    public ApplicantTypeResponseDto updateApplicantType(Long id, ApplicantTypeRequestDto dto) {
        ApplicantType entity = applicantTypeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant type not found", "ERR_APPLICANT_TYPE_NOT_FOUND"));

        if (applicantTypeRepository.existsByNameIgnoreCaseAndIsDeletedFalseAndIdNot(dto.getName().trim(), id)) {
            throw new ValidationException("Another applicant type with name '" + dto.getName() + "' already exists",
                    "ERR_DUPLICATE_APPLICANT_TYPE");
        }

        entity.setName(dto.getName().trim());
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
        // Page starts from 1 → convert to 0-based for JPA
        page = Math.max(page, 1);
        size = size > 0 ? size : 10;

        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by("name").ascending());

        return applicantTypeRepository.findAll(pageRequest)
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


}