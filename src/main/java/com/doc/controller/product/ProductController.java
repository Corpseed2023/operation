package com.doc.controller.product;

import com.doc.dto.product.ProductRequestDto;
import com.doc.dto.product.ProductResponseDto;
import com.doc.exception.ValidationException;
import com.doc.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody ProductRequestDto requestDto
    ) {
        logger.info("Creating product for request: {}", requestDto);

        ProductResponseDto response = productService.createProduct(requestDto);

        logger.info("Product created successfully with ID: {}", response.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        logger.info("Fetching product with ID: {}", id);
        ProductResponseDto response = productService.getProductById(id);
        logger.info("Retrieved product with ID: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("Fetching active and non-deleted products, page: {}, size: {}", page, size);
        List<ProductResponseDto> responses = productService.getAllProducts(userId, page, size);
        logger.info("Retrieved {} active and non-deleted products", responses.size());
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        logger.info("Deleting product with ID: {}", id);
        productService.deleteProduct(id);
        logger.info("Product deleted successfully with ID: {}", id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Update an existing product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation failed"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "Product or user not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto requestDto) {

        logger.info("Updating product with ID: {}, request: {}", id, requestDto);

        // Optional: you can enforce that productId in DTO matches path id (recommended)
        if (requestDto.getProductId() != null && !requestDto.getProductId().equals(id)) {
            throw new ValidationException("Product ID in body must match path ID", "ID_MISMATCH");
        }

        // Usually we don't require productId in update body → set it from path
        requestDto.setProductId(id);

        ProductResponseDto updated = productService.updateProduct(id, requestDto);

        logger.info("Product updated successfully with ID: {}", updated.getId());
        return ResponseEntity.ok(updated);
    }
}