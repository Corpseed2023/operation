package com.doc.controller.product;

import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsRequestDto;
import com.doc.dto.productRequiredDocument.ProductRequiredDocumentsResponseDto;
import com.doc.service.ProductRequiredDocumentsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/required-documents")
@Validated
public class ProductRequiredDocumentsController {

    @Autowired
    private ProductRequiredDocumentsService requiredDocumentsService;

    @PostMapping
    public ResponseEntity<List<ProductRequiredDocumentsResponseDto>> createRequiredDocuments(
            @Valid @RequestBody List<ProductRequiredDocumentsRequestDto> requestDtoList) {
        List<ProductRequiredDocumentsResponseDto> responses = requiredDocumentsService.createRequiredDocuments(requestDtoList);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductRequiredDocumentsResponseDto> getRequiredDocumentById(
            @PathVariable Long id,
            @RequestParam Long userId) {
        ProductRequiredDocumentsResponseDto response = requiredDocumentsService.getRequiredDocumentById(id, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<ProductRequiredDocumentsResponseDto>> getAllRequiredDocuments(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String centralName,
            @RequestParam(required = false) String stateName) {
        List<ProductRequiredDocumentsResponseDto> responses = requiredDocumentsService.getAllRequiredDocuments(userId, page, size, name, type, country, centralName, stateName);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductRequiredDocumentsResponseDto> updateRequiredDocument(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequiredDocumentsRequestDto requestDto) {
        ProductRequiredDocumentsResponseDto response = requiredDocumentsService.updateRequiredDocument(id, requestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequiredDocument(@PathVariable Long id) {
        requiredDocumentsService.deleteRequiredDocument(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}