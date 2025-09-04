package com.doc.service;


import com.doc.dto.company.CompanyRequestDto;
import com.doc.dto.company.CompanyResponseDto;

import java.util.List;

public interface CompanyService {

    CompanyResponseDto createCompany(CompanyRequestDto requestDto);

    CompanyResponseDto getCompanyById(Long id);


    CompanyResponseDto updateCompany(Long id, CompanyRequestDto requestDto);

    void deleteCompany(Long id);

    List<CompanyResponseDto> getAllCompanies(int page, int size, Long userId);
}
