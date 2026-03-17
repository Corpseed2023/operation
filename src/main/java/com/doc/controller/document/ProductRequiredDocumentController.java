package com.doc.controller.document;

import com.doc.dto.document.ProductRequiredDocumentRequestDto;
import com.doc.dto.document.ProductRequiredDocumentResponseDto;
import com.doc.service.ProductRequiredDocumentService;
import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/product-required-documents")
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

    // Active only, paginated (if needed)
    @Operation(summary = "Get active required documents (paginated)")
    @GetMapping("/active/{userId}")
    public ResponseEntity<List<ProductRequiredDocumentResponseDto>> getActivePaginated(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productRequiredDocumentService.getActivePaginated(userId,page, size));
    }

    @PostMapping("/import-required-document")
    @Operation(summary = "Bulk import required document templates from CSV stored in S3")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Import completed – returns successfully imported records"),
            @ApiResponse(responseCode = "400", description = "Invalid file format, empty file or bad S3 URL"),
            @ApiResponse(responseCode = "422", description = "Validation errors – some or all rows failed")
    })
    public ResponseEntity<List<ProductRequiredDocumentResponseDto>> importFromS3(
            @RequestParam("s3Url") String s3Url,
            @RequestParam(value = "createdBy", required = true) Long createdBy) {

        if (StringUtils.isBlank(s3Url)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "s3Url is required");
        }

        List<ProductRequiredDocumentResponseDto> result =
                productRequiredDocumentService.importFromS3(s3Url, createdBy);

        return ResponseEntity.ok(result);
    }

}



