package com.doc.repository;

import com.doc.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing {@link Product} entities.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Checks if a product with the given name exists and is not deleted.
     *
     * @param productName the product name to check
     * @return true if a product with the specified name exists and is not deleted, false otherwise
     */
    boolean existsByProductNameAndIsDeletedFalse(String productName);

    /**
     * Finds all active and non-deleted products with pagination.
     *
     * @param pageable pagination information
     * @return a page of active and non-deleted products
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false")
    Page<Product> findByIsActiveTrueAndIsDeletedFalse(Pageable pageable);

    /**
     * Finds a product by ID if not deleted.
     *
     * @param id the product ID
     * @return an Optional containing the product if found and not deleted, empty otherwise
     */
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Product> findByIdAndIsDeletedFalse(@Param("id") Long id);
}