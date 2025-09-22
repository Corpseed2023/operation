package com.doc.service;


import com.doc.dto.productMilestoneMap.ProductMilestoneMapRequestDto;
import com.doc.dto.productMilestoneMap.ProductMilestoneMapResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing ProductMilestoneMap entities.
 */
public interface ProductMilestoneMapService {

    /**
     * Creates a new product-milestone mapping.
     *
     * @param requestDto the mapping data to create
     * @return the created mapping
     */
    ProductMilestoneMapResponseDto createProductMilestoneMap(ProductMilestoneMapRequestDto requestDto);

    /**
     * Updates an existing product-milestone mapping.
     *
     * @param id the mapping ID
     * @param requestDto the updated mapping data
     * @return the updated mapping
     */
    ProductMilestoneMapResponseDto updateProductMilestoneMap(Long id, ProductMilestoneMapRequestDto requestDto);

    /**
     * Retrieves a product-milestone mapping by ID.
     *
     * @param id the mapping ID
     * @return the mapping
     */
    ProductMilestoneMapResponseDto getProductMilestoneMapById(Long id);

    /**
     * Deletes a product-milestone mapping by ID.
     *
     * @param id the mapping ID
     */
    void deleteProductMilestoneMap(Long id);


}