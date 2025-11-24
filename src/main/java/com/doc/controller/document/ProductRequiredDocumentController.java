package com.doc.controller.document;

import com.doc.dto.document.ProductRequiredDocumentRequestDto;
import com.doc.dto.document.ProductRequiredDocumentResponseDto;
import com.doc.service.ProductRequiredDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-required-documents")
public class ProductRequiredDocumentController {

    @Autowired
    private ProductRequiredDocumentService productRequiredDocumentService   ;

    @Operation(summary = "Create a new required document template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Required document created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Duplicate name + location combination")
    })
    @PostMapping
    public ResponseEntity<ProductRequiredDocumentResponseDto> create(
            @Valid @RequestBody ProductRequiredDocumentRequestDto dto) {
        ProductRequiredDocumentResponseDto response = productRequiredDocumentService.create(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing required document template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated successfully"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate name + location")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductRequiredDocumentResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequiredDocumentRequestDto dto) {
        ProductRequiredDocumentResponseDto response = productRequiredDocumentService.update(id, dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Soft delete a required document template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productRequiredDocumentService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get required document by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductRequiredDocumentResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productRequiredDocumentService.getById(id));
    }

    @Operation(summary = "Get all required documents (paginated) - includes inactive")
    @GetMapping
    public ResponseEntity<Page<ProductRequiredDocumentResponseDto>> getAllPaged(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productRequiredDocumentService.getAllPaged(page, size));
    }

    @Operation(summary = "Get all active required documents (paginated)")
    @GetMapping("/active")
    public ResponseEntity<Page<ProductRequiredDocumentResponseDto>> getActivePaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productRequiredDocumentService.getAllActivePaged(page, size));
    }

    @Operation(summary = "Get all active required documents (no pagination) - for dropdowns")
    @GetMapping("/active/list")
    public ResponseEntity<List<ProductRequiredDocumentResponseDto>> getAllActiveList() {
        return ResponseEntity.ok(productRequiredDocumentService.getAllActive());
    }
}