package com.doc.controller.document;

import com.doc.dto.document.ProductRequiredDocumentRequestDto;
import com.doc.dto.document.ProductRequiredDocumentResponseDto;
import com.doc.service.ProductRequiredDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/product-required-documents")
public class ProductRequiredDocumentController {

    @Autowired
    private ProductRequiredDocumentService productRequiredDocumentService;

    @Operation(summary = "Create a new required document template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Duplicate name + location combination")
    })
    @PostMapping
    public ResponseEntity<ProductRequiredDocumentResponseDto> create(
            @Valid @RequestBody ProductRequiredDocumentRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productRequiredDocumentService.create(dto));
    }

    @Operation(summary = "Update an existing required document template")
    @PutMapping("/{id}")
    public ResponseEntity<ProductRequiredDocumentResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequiredDocumentRequestDto dto) {
        return ResponseEntity.ok(productRequiredDocumentService.update(id, dto));
    }

    @Operation(summary = "Get active required documents (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of active required document templates"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters or missing/invalid userId"),
            @ApiResponse(responseCode = "404", description = "User not found or inactive")
    })
    @GetMapping("/active")
    public ResponseEntity<List<ProductRequiredDocumentResponseDto>> getActivePaginated(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = true) Long userId) {          // ← added here

        // Basic input validation (optional but recommended)
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid userId is required"
            );
        }

        // Call service with userId
        List<ProductRequiredDocumentResponseDto> documents =
                productRequiredDocumentService.getActivePaginated(page, size, userId);

        return ResponseEntity.ok(documents);
    }


}



