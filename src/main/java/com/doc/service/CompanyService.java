package com.doc.service;


import com.doc.dto.company.CompanyRequestDto;
import com.doc.dto.company.CompanyResponseDto;
import jakarta.validation.Valid;

import java.util.List;

public interface CompanyService {
    CompanyResponseDto createCompany(@Valid CompanyRequestDto requestDto, Long companyId);
    CompanyResponseDto getCompanyById(Long companyId);

}
