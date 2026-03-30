package com.doc.controller.product;

import com.doc.dto.product.ProductRequestDto;
import com.doc.dto.product.ProductResponseDto;
import com.doc.dto.product.request.ProductUpdateDto;
import com.doc.service.ProductService;
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
@RequestMapping("/operationService/api/products")
@Validated
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseEntity<List<ProductResponseDto>> createProducts(@Valid @RequestBody List<ProductRequestDto> requestDtoList) {
        logger.info("Creating products for request: {}", requestDtoList);
        List<ProductResponseDto> responses = productService.createProducts(requestDtoList);
        logger.info("Created {} products successfully", responses.size());
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
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


    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateDto updateDto,
            @RequestParam Long userId) {   // ← same pattern as getAllProducts

        logger.info("Received update request for product ID: {} by user: {}", id, userId);

        ProductResponseDto updated = productService.updateProduct(id, updateDto, userId);

        return ResponseEntity.ok(updated);
    }


}