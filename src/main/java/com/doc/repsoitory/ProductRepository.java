package com.doc.repsoitory;

import com.doc.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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
     * Finds all non-deleted products with pagination.
     *
     * @param pageable pagination information
     * @return a page of non-deleted products
     */
    Page<Product> findByIsDeletedFalse(Pageable pageable);

    /**
     * Finds products by filters (name, active status, date range) with pagination.
     *
     * @param productName the product name to filter by (optional)
     * @param isActive    the active status to filter by (optional)
     * @param startDate   the start date to filter by (optional)
     * @param endDate     the end date to filter by (optional)
     * @param pageable    pagination information
     * @return a page of non-deleted products matching the filters
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:productName IS NULL OR p.productName LIKE %:productName%) AND " +
            "(:isActive IS NULL OR p.isActive = :isActive) AND " +
            "(:startDate IS NULL OR p.date >= :startDate) AND " +
            "(:endDate IS NULL OR p.date <= :endDate) AND " +
            "p.isDeleted = false")
    Page<Product> findByFilters(
            @Param("productName") String productName,
            @Param("isActive") Boolean isActive,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Finds products associated with a specific required document ID and not deleted.
     *
     * @param documentId the document ID
     * @return a list of non-deleted products associated with the document
     */
    @Query("SELECT p FROM Product p JOIN p.requiredDocuments d WHERE d.id = :documentId AND p.isDeleted = false")
    List<Product> findByRequiredDocumentsId(@Param("documentId") Long documentId);

    /**
     * Finds a product by ID if not deleted.
     *
     * @param id the product ID
     * @return an Optional containing the product if found and not deleted, empty otherwise
     */
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Product> findByIdAndIsDeletedFalse(@Param("id") Long id);
}
