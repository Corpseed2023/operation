package com.doc.controller.document;

import com.doc.dto.productRequiredDocument.GetRequiredDocumentsByProductRequestDto;
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


    @PutMapping("/{id}")
    public ResponseEntity<ProductRequiredDocumentsResponseDto> updateRequiredDocument(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequiredDocumentsRequestDto requestDto) {
        ProductRequiredDocumentsResponseDto response = requiredDocumentsService.updateRequiredDocument(id, requestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
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

    @PostMapping("/product/{productId}")
    public ResponseEntity<List<ProductRequiredDocumentsResponseDto>> getRequiredDocumentsByProduct(
            @PathVariable Long productId,
            @Valid @RequestBody GetRequiredDocumentsByProductRequestDto requestDto) {
        List<ProductRequiredDocumentsResponseDto> responses =
                requiredDocumentsService.getRequiredDocumentsByProduct(
                        productId, requestDto.getProjectId(), requestDto.getStateName(), requestDto.getCentralName());
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    @GetMapping("/admin/product/{productId}")
    public ResponseEntity<List<ProductRequiredDocumentsResponseDto>> getRequiredDocumentsForAdmin(
            @PathVariable Long productId,
            @RequestParam Long userId,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String centralName) {
        List<ProductRequiredDocumentsResponseDto> responses =
                requiredDocumentsService.getRequiredDocumentsForAdmin(productId, userId, stateName, centralName);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }
}