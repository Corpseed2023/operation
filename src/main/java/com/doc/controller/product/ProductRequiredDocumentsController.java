package com.doc.controller.product;

import com.doc.dto.productRequiredDocument.GetAllRequiredDocumentsRequestDto;
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
            @Valid @RequestBody GetAllRequiredDocumentsRequestDto requestDto) {
        List<ProductRequiredDocumentsResponseDto> responses = requiredDocumentsService.getAllRequiredDocuments(
                requestDto.getUserId(),
                requestDto.getPage(),
                requestDto.getSize(),
                requestDto.getName(),
                requestDto.getType(),
                requestDto.getCountry(),
                requestDto.getCentralName(),
                requestDto.getStateName(),
                requestDto.getProductId() // Add productId
        );
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

    @GetMapping("/project/{projectId}/product/{productId}")
    public ResponseEntity<List<ProductRequiredDocumentsResponseDto>> getRequiredDocumentsByProjectAndProduct(
            @PathVariable Long projectId,
            @PathVariable Long productId,
            @RequestParam Long userId) {
        List<ProductRequiredDocumentsResponseDto> responses =
                requiredDocumentsService.getRequiredDocumentsByProjectAndProduct(projectId, productId, userId);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }
}