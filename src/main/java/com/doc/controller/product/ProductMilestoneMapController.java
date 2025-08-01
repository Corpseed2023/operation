package com.doc.controller.product;

import com.doc.dto.productMilestoneMap.ProductMilestoneMapRequestDto;
import com.doc.dto.productMilestoneMap.ProductMilestoneMapResponseDto;
import com.doc.service.ProductMilestoneMapService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing ProductMilestoneMap entities.
 */
@RestController
@RequestMapping("/api/product-milestone-maps")
public class ProductMilestoneMapController {

    @Autowired
    private ProductMilestoneMapService productMilestoneMapService;

    /**
     * Creates a new product-milestone mapping.
     *
     * @param requestDto the mapping data to create
     * @return the created mapping
     */
    @PostMapping
    public ResponseEntity<ProductMilestoneMapResponseDto> createProductMilestoneMap(
            @Valid @RequestBody ProductMilestoneMapRequestDto requestDto) {
        ProductMilestoneMapResponseDto response = productMilestoneMapService.createProductMilestoneMap(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Updates an existing product-milestone mapping.
     *
     * @param id the mapping ID
     * @param requestDto the updated mapping data
     * @return the updated mapping
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductMilestoneMapResponseDto> updateProductMilestoneMap(
            @PathVariable Long id,
            @Valid @RequestBody ProductMilestoneMapRequestDto requestDto) {
        ProductMilestoneMapResponseDto response = productMilestoneMapService.updateProductMilestoneMap(id, requestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieves a product-milestone mapping by ID.
     *
     * @param id the mapping ID
     * @return the mapping if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductMilestoneMapResponseDto> getProductMilestoneMapById(@PathVariable Long id) {
        ProductMilestoneMapResponseDto response = productMilestoneMapService.getProductMilestoneMapById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieves all product-milestone mappings with pagination.
     *
     * @param pageable pagination information
     * @return a page of mappings
     */
    @GetMapping
    public ResponseEntity<Page<ProductMilestoneMapResponseDto>> getAllProductMilestoneMaps(Pageable pageable) {
        Page<ProductMilestoneMapResponseDto> mappings = productMilestoneMapService.getAllProductMilestoneMaps(pageable);
        return new ResponseEntity<>(mappings, HttpStatus.OK);
    }

    /**
     * Deletes a product-milestone mapping by ID.
     *
     * @param id the mapping ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductMilestoneMap(@PathVariable Long id) {
        productMilestoneMapService.deleteProductMilestoneMap(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieves product-milestone mappings by product ID with pagination.
     *
     * @param productId the product ID
     * @param pageable pagination information
     * @return a page of mappings for the product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ProductMilestoneMapResponseDto>> getProductMilestoneMapsByProduct(
            @PathVariable Long productId, Pageable pageable) {
        Page<ProductMilestoneMapResponseDto> mappings = productMilestoneMapService.getProductMilestoneMapsByProduct(productId, pageable);
        return new ResponseEntity<>(mappings, HttpStatus.OK);
    }
}