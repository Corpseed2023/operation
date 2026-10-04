package com.doc.controller.document;

import com.doc.dto.document.ProductDocumentMappingRequestDto;
import com.doc.dto.document.ProductDocumentMappingResponseDto;
import com.doc.exception.ValidationException;
import com.doc.service.ProductDocumentMappingService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operationService/api/products")
@RequiredArgsConstructor
@Tag(
        name = "Product Document Mapping",
        description = "Manage required documents per product and multiple applicant types"
)
public class ProductDocumentMappingController {

    private static final Logger logger =
            LogManager.getLogger(ProductDocumentMappingController.class);

    private final ProductDocumentMappingService mappingService;

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Documents assigned successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid document mapping request"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product, applicant type or required document not found"
            )
    })
    @PostMapping("/{productId}/documents/map")
    public ResponseEntity<String> assignDocuments(
            @PathVariable
            @Parameter(description = "Product ID")
            Long productId,

            @Valid
            @RequestBody
            ProductDocumentMappingRequestDto request
    ) {

        logger.info(
                "Document mapping API called. pathProductId={}, bodyProductId={}, "
                        + "applicantTypeIds={}, requiredDocumentIds={}, updatedBy={}",
                productId,
                request.getProductId(),
                request.getApplicantTypeIds(),
                request.getRequiredDocumentIds(),
                request.getUpdatedBy()
        );

        if (request.getProductId() == null
                || !productId.equals(request.getProductId())) {

            logger.warn(
                    "Product ID mismatch. pathProductId={}, bodyProductId={}",
                    productId,
                    request.getProductId()
            );

            throw new ValidationException(
                    "Product ID in URL ("
                            + productId
                            + ") does not match Product ID in request body ("
                            + request.getProductId()
                            + "). Please use the same Product ID in both places.",
                    "ERR_PRODUCT_ID_MISMATCH"
            );
        }

        mappingService.assignDocuments(request);

        logger.info(
                "Document mapping API completed successfully. "
                        + "productId={}, applicantTypeIds={}, requiredDocumentIds={}",
                productId,
                request.getApplicantTypeIds(),
                request.getRequiredDocumentIds()
        );

        return ResponseEntity.ok(
                "Required documents assigned successfully to selected applicant types."
        );
    }

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Documents retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid product or applicant type ID"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product or applicant type not found"
            )
    })
    @GetMapping("/{productId}/documents")
    public ResponseEntity<List<ProductDocumentMappingResponseDto>>
    getRequiredDocuments(

            @PathVariable
            @Parameter(description = "Product ID")
            Long productId,

            @RequestParam(required = false)
            @Parameter(
                    description =
                            "Applicant Type ID. "
                                    + "When provided, documents for that applicant type are returned. "
                                    + "When omitted, all active document mappings for the product are returned."
            )
            Long applicantTypeId
    ) {

        logger.debug(
                "Fetching product documents. productId={}, applicantTypeId={}",
                productId,
                applicantTypeId
        );

        List<ProductDocumentMappingResponseDto> documents =
                mappingService.getRequiredDocuments(
                        productId,
                        applicantTypeId
                );

        logger.debug(
                "Product documents fetched successfully. "
                        + "productId={}, applicantTypeId={}, count={}",
                productId,
                applicantTypeId,
                documents.size()
        );

        return ResponseEntity.ok(documents);
    }


    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents updated successfully"),
            @ApiResponse(responseCode = "400", description = "Product ID mismatch or invalid request"),
            @ApiResponse(responseCode = "404", description = "Product or required documents not found")
    })
    @PutMapping("/{productId}/documents/map")
    public ResponseEntity<String> updateDocuments(
            @PathVariable @Parameter(description = "Product ID") Long productId,
            @Valid @RequestBody ProductDocumentMappingRequestDto request) {

        if (!productId.equals(request.getProductId())) {
            return ResponseEntity.badRequest()
                    .body("Product ID in path (" + productId + ") must match body (" + request.getProductId() + ")");
        }

        mappingService.updateDocuments(request);
        return ResponseEntity.ok("Required documents updated successfully");
    }



}