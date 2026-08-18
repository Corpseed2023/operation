package com.doc.controller.company;


import com.doc.dto.company.MyCompanyDocumentRequestDto;
import com.doc.dto.company.MyCompanyDocumentResponseDto;
import com.doc.service.MyCompanyDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/v1/my-company-documents")
@RequiredArgsConstructor
public class MyCompanyDocumentController {

    private final MyCompanyDocumentService myCompanyDocumentService;

    @PostMapping
    public ResponseEntity<MyCompanyDocumentResponseDto> uploadOrReplace(
            @Valid @RequestBody MyCompanyDocumentRequestDto request,
            @RequestParam Long currentUserId // swap for @AuthenticationPrincipal once auth is wired in
    ) {
        return ResponseEntity.ok(myCompanyDocumentService.uploadOrReplace(request, currentUserId));
    }

    @GetMapping
    public ResponseEntity<List<MyCompanyDocumentResponseDto>> getAll() {
        return ResponseEntity.ok(myCompanyDocumentService.getAll());
    }

    @GetMapping("/{requiredDocumentId}")
    public ResponseEntity<MyCompanyDocumentResponseDto> getByRequiredDocumentId(
            @PathVariable Long requiredDocumentId) {
        return ResponseEntity.ok(myCompanyDocumentService.getByRequiredDocumentId(requiredDocumentId));
    }
}