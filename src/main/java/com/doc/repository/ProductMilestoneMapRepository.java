package com.doc.repository;

import com.doc.entity.product.ProductMilestoneMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing ProductMilestoneMap entities.
 */
@Repository
public interface ProductMilestoneMapRepository extends JpaRepository<ProductMilestoneMap, Long> {

    /**
     * Checks if a mapping with the given product ID and order exists.
     *
     * @param productId the product ID
     * @param order the step order
     * @return true if a mapping exists, false otherwise
     */
    boolean existsByProductIdAndOrder(Long productId, int order);

    /**
     * Finds mappings by product ID with pagination.
     *
     * @param productId the product ID
     * @param pageable pagination information
     * @return a page of mappings for the product
     */
    Page<ProductMilestoneMap> findByProductId(Long productId, Pageable pageable);
}