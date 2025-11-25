package com.doc.controller.document;

import com.doc.dto.document.ProductDocumentMappingGroupedDto;
import com.doc.dto.document.ProductDocumentMappingRequestDto;
import com.doc.dto.document.ProductDocumentMappingResponseDto;
import com.doc.dto.document.ProductDocumentRequirementResponseDto;
import com.doc.service.ProductDocumentMappingService;
import io.swagger.v3.oas.annotations.Operation;
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
//
//    @Operation(summary = "Assign or update required documents for a product",
//            description = "Replaces all existing document mappings for the given product and applicant type combination. " +
//                    "Use applicantTypeId = null for global/common documents.")
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

//    @Operation(summary = "Get required documents for a product",
//            description = "Returns documents specific to applicantTypeId. If applicantTypeId is null or omitted, " +
//                    "returns global/common documents.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Documents retrieved successfully"))
    @GetMapping("/{productId}/documents")
    public ResponseEntity<List<ProductDocumentMappingResponseDto>> getRequiredDocuments(
            @PathVariable @Parameter(description = "Product ID") Long productId,
            @RequestParam(required = false) @Parameter(description = "Applicant Type ID (optional, null = global)") Long applicantTypeId) {

        List<ProductDocumentMappingResponseDto> documents =
                mappingService.getRequiredDocuments(productId, applicantTypeId);

        return ResponseEntity.ok(documents);
    }

//    @Operation(summary = "Get all document mappings grouped by applicant type",
//            description = "Admin helper endpoint. Returns all mappings (global + per applicant type) grouped neatly for UI display.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Grouped mappings retrieved"))
    @GetMapping("/{productId}/documents/grouped")
    public ResponseEntity<List<ProductDocumentMappingGroupedDto>> getAllMappingsGrouped(
            @PathVariable @Parameter(description = "Product ID") Long productId) {

        return ResponseEntity.ok(mappingService.getAllMappingsGroupedByApplicantType(productId));
    }

    // Add this method to your controller
//    @Operation(summary = "Get final list of required documents for a Product + Applicant Type",
//            description = """
//          Returns the complete, merged list of documents a customer must upload.
//          • Includes global/common documents (applicantTypeId = null)
//          • Includes applicant-type specific documents
//          • De-duplicated by requiredDocumentId
//          • Sorted by displayOrder (specific overrides global)
//          """)
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Required documents retrieved"))
    @GetMapping("/{productId}/documents/required")
    public ResponseEntity<List<ProductDocumentMappingResponseDto>> getFinalRequiredDocuments(
            @PathVariable @Parameter(description = "Product ID") Long productId,
            @RequestParam @Parameter(description = "Applicant Type ID (e.g., Brand Owner, Importer)", required = true) Long applicantTypeId) {

        List<ProductDocumentMappingResponseDto> finalDocs =
                mappingService.getFinalRequiredDocuments(productId, applicantTypeId);

        return ResponseEntity.ok(finalDocs);
    }

//    @Operation(summary = "Get complete document requirements for a Product grouped by Applicant Type",
//            description = """
//          Returns ALL required documents for a product, perfectly grouped by Applicant Type.
//          • "Common Documents" group first (global docs)
//          • Then one group per active Applicant Type
//          • Each group contains only the documents specific to that type + fallback to common
//          • De-duplicated and properly ordered
//          • This is the main API used in project creation / customer portal
//          """)
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Document requirements retrieved"))
    @GetMapping("/{productId}/documents/requirements")
    public ResponseEntity<ProductDocumentRequirementResponseDto> getDocumentRequirementsByApplicantType(
            @PathVariable @Parameter(description = "Product ID") Long productId) {

        ProductDocumentRequirementResponseDto response =
                mappingService.getDocumentRequirementsGrouped(productId);

        return ResponseEntity.ok(response);
    }
}