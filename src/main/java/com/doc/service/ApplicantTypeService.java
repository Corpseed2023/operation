package com.doc.service;// package com.doc.service;

import com.doc.entity.document.ApplicantType;
import java.util.List;

public interface ApplicantTypeService {

    ApplicantType createApplicantType(ApplicantType applicantType);

    List<ApplicantType> getAllActiveApplicantTypes();

    ApplicantType getApplicantTypeById(Long id);

    ApplicantType updateApplicantType(Long id, ApplicantType applicantType);

    void softDeleteApplicantType(Long id);
}