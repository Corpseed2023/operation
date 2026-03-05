package com.doc.controller.company;

import com.doc.dto.company.CompanyRequestDto;
import com.doc.dto.company.CompanyResponseDto;
import com.doc.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/companies")
@Validated
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @PostMapping("/createCompany")
    public ResponseEntity<CompanyResponseDto> createCompany(
            @Valid @RequestBody CompanyRequestDto requestDto,
            @RequestParam(required = true) Long companyId) {

        CompanyResponseDto response = companyService.createCompany(requestDto, companyId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


}
