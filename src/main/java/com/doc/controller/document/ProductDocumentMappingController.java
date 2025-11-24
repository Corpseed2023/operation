//package com.doc.controller.document;
//
//import com.doc.dto.document.ProductDocumentMappingRequestDto;
//import com.doc.service.ProductDocumentMappingService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/products")
//@RequiredArgsConstructor
//public class ProductDocumentMappingController {
//
//    private final ProductDocumentMappingService mappingService;
//
//    /**
//     * Assign / Update required documents for a Product + Applicant Type
//     * Replaces all existing mappings for this combination
//     */
//    @PostMapping("/{productId}/documents/map")
//    public ResponseEntity<String> assignDocuments(
//            @PathVariable Long productId,
//            @RequestBody @Valid ProductDocumentMappingRequestDto request) {
//
//        // Optional: validate productId matches request.body productId (security)
//        if (!productId.equals(request.productId())) {
//            return ResponseEntity.badRequest()
//                    .body("Product ID in path and body must match");
//        }
//
//        mappingService.assignDocuments(request);
//        return ResponseEntity.ok("Documents assigned successfully");
//    }
//
//    /**
//     * Get all required documents for a specific Product + Applicant Type
//     * applicantTypeId = null → returns global (common) documents
//     */
//    @GetMapping("/{productId}/documents")
//    public ResponseEntity<List<ProductDocumentMappingResponseDto>> getRequiredDocuments(
//            @PathVariable Long productId,
//            @RequestParam(required = false) Long applicantTypeId) {
//
//        List<ProductDocumentMappingResponseDto> documents =
//                mappingService.getRequiredDocuments(productId, applicantTypeId);
//
//        return ResponseEntity.ok(documents);
//    }
//
//    /**
//     * Get all mappings grouped by Applicant Type for a Product (Admin UI helper)
//     */
//    @GetMapping("/{productId}/documents/grouped")
//    public ResponseEntity<?> getAllMappingsForProduct(@PathVariable Long productId) {
//        return ResponseEntity.ok(mappingService.getAllMappingsGroupedByApplicantType(productId));
//    }
//}