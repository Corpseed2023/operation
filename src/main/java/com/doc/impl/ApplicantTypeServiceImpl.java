package com.doc.impl;// package com.doc.impl;

import com.doc.entity.document.ApplicantType;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ApplicantTypeRepository;
import com.doc.service.ApplicantTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ApplicantTypeServiceImpl implements ApplicantTypeService {

    @Autowired
    private ApplicantTypeRepository applicantTypeRepository;

    @Override
    public ApplicantType createApplicantType(ApplicantType applicantType) {
        validateApplicantType(applicantType);

        if (applicantTypeRepository.existsByNameIgnoreCaseAndIsActiveTrue(applicantType.getName())) {
            throw new ValidationException("Applicant type with name '" + applicantType.getName() + "' already exists", "DUPLICATE_APPLICANT_TYPE");
        }

        applicantType.setActive(true);
        return applicantTypeRepository.save(applicantType);
    }

    @Override
    public List<ApplicantType> getAllActiveApplicantTypes() {
        return applicantTypeRepository.findAllByIsActiveTrueOrderByName();
    }

    @Override
    public ApplicantType getApplicantTypeById(Long id) {
        return applicantTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant type with ID " + id + " not found or inactive", "APPLICANT_TYPE_NOT_FOUND"));
    }

    @Override
    public ApplicantType updateApplicantType(Long id, ApplicantType applicantTypeDetails) {
        ApplicantType existing = getApplicantTypeById(id);

        if (!existing.getName().equalsIgnoreCase(applicantTypeDetails.getName())) {
            if (applicantTypeRepository.existsByNameIgnoreCaseAndIdNotAndIsActiveTrue(applicantTypeDetails.getName(), id)) {
                throw new ValidationException("Another applicant type with name '" + applicantTypeDetails.getName() + "' already exists", "DUPLICATE_APPLICANT_TYPE");
            }
        }

        validateApplicantType(applicantTypeDetails);

        existing.setName(applicantTypeDetails.getName().trim());
        existing.setDescription(applicantTypeDetails.getDescription());

        return applicantTypeRepository.save(existing);
    }

    @Override
    public void softDeleteApplicantType(Long id) {
        ApplicantType applicantType = getApplicantTypeById(id);
        applicantType.setActive(false);
        applicantTypeRepository.save(applicantType);
    }

    private void validateApplicantType(ApplicantType applicantType) {
        if (applicantType.getName() == null || applicantType.getName().trim().isEmpty()) {
            throw new ValidationException("Applicant type name cannot be empty", "INVALID_APPLICANT_TYPE_NAME");
        }
    }
}