package com.doc.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "LEAD-SERVICE", url = "http://localhost:9001")
public interface LeadFeignClient {

    @GetMapping("/leadService/api/v1/getSolutionByIdOnly/{solutionId}")
    Map<String, Object> getSolutionByIdOnly(
            @PathVariable("solutionId") Long solutionId
    );
}