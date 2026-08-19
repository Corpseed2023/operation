package com.doc.controller.company;

import com.doc.dto.company.MyCompanyDocumentRequestDto;
import com.doc.dto.company.MyCompanyDocumentResponseDto;
import com.doc.service.MyCompanyDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/company-documents")
@RequiredArgsConstructor
public class MyCompanyDocumentController {

    private final MyCompanyDocumentService myCompanyDocumentService;

    @PostMapping
    public ResponseEntity<MyCompanyDocumentResponseDto> upload(
            @Valid @RequestBody MyCompanyDocumentRequestDto request,
            @RequestParam Long userId) {

        MyCompanyDocumentResponseDto response = myCompanyDocumentService.upload(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MyCompanyDocumentResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody MyCompanyDocumentRequestDto request,
            @RequestParam Long userId) {

        MyCompanyDocumentResponseDto response = myCompanyDocumentService.update(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<MyCompanyDocumentResponseDto>> getAll() {
        return ResponseEntity.ok(myCompanyDocumentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MyCompanyDocumentResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(myCompanyDocumentService.getById(id));
    }

    @GetMapping("/type/{documentType}")
    public ResponseEntity<List<MyCompanyDocumentResponseDto>> getByType(@PathVariable String documentType) {
        return ResponseEntity.ok(myCompanyDocumentService.getByType(documentType));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        myCompanyDocumentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}