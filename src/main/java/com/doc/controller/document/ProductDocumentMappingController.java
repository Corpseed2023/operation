package com.doc.controller.document;

import com.doc.dto.document.ProductDocumentMappingGroupedDto;
import com.doc.dto.document.ProductDocumentMappingRequestDto;
import com.doc.dto.document.ProductDocumentMappingResponseDto;
import com.doc.dto.document.ProductDocumentRequirementResponseDto;
import com.doc.service.ProductDocumentMappingService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Document Mapping", description = "Manage required documents per product and applicant type")
public class ProductDocumentMappingController {

    private final ProductDocumentMappingService mappingService;

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Product ID mismatch or invalid request"),
            @ApiResponse(responseCode = "404", description = "Product or required documents not found")
    })
    @PostMapping("/{productId}/documents/map")
    public ResponseEntity<String> assignDocuments(
            @PathVariable @Parameter(description = "Product ID") Long productId,
            @Valid @RequestBody ProductDocumentMappingRequestDto request) {

        if (!productId.equals(request.getProductId())) {
            return ResponseEntity.badRequest()
                    .body("Product ID in path (" + productId + ") must match body (" + request.getProductId() + ")");
        }




        mappingService.assignDocuments(request);
        return ResponseEntity.ok("Required documents assigned successfully");
    }

    @ApiResponses(@ApiResponse(responseCode = "200", description = "Documents retrieved successfully"))
    @GetMapping("/{productId}/documents")
    public ResponseEntity<List<ProductDocumentMappingResponseDto>> getRequiredDocuments(
            @PathVariable @Parameter(description = "Product ID") Long productId,
            @RequestParam(required = false) @Parameter(description = "Applicant Type ID (optional, null = global)") Long applicantTypeId) {

        List<ProductDocumentMappingResponseDto> documents =
                mappingService.getRequiredDocuments(productId, applicantTypeId);

        return ResponseEntity.ok(documents);
    }

}