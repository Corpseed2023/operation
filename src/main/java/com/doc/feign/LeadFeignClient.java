package com.doc.feign;

import com.doc.dto.LeadDTO;
import com.doc.dto.legalDashbaord.CompanyLegalClientDto;
import com.doc.dto.vendor.LeadVendorAssigneeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "LEAD-SERVICE", url = "http://localhost:9001")
public interface LeadFeignClient {

    @GetMapping("/leadService/api/v1/getSolutionByIdOnly/{solutionId}")
    Map<String, Object> getSolutionByIdOnly(
            @PathVariable("solutionId") Long solutionId
    );

    @GetMapping("/leadService/api/company-legal-verification/pending")
    List<CompanyLegalClientDto> getPendingCompanyLegalRequests(
            @RequestParam("userId") Long userId
    );

    @GetMapping("/internal/vendors/assignee/by-lead/{leadId}")
    LeadVendorAssigneeDto getVendorAssigneeByLead(
            @PathVariable("leadId") Long leadId
    );

    @PostMapping("/leadService/api/v1/lead/createLead")
    ResponseEntity<Object> createLead(@RequestBody LeadDTO leadDTO);
}