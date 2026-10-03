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
 * Repository interface for managing Product entities.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Checks if a product with the given name exists and is not deleted.
     *
     * @param productName the product name to check
     * @return true if a product with the specified name exists and is not deleted
     */
    boolean existsByProductNameAndIsDeletedFalse(String productName);

    /**
     * Finds all active and non-deleted products with pagination.
     *
     * @param pageable pagination information
     * @return page of active and non-deleted products
     */
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.isActive = true
              AND p.isDeleted = false
            """)
    Page<Product> findByIsActiveTrueAndIsDeletedFalse(Pageable pageable);

    /**
     * Finds a product by ID if not deleted.
     *
     * NOTE:
     * Method name is old/misleading, but kept unchanged
     * to avoid breaking existing code.
     *
     * @param id product ID
     * @return product if found and not deleted
     */
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.id = :id
              AND p.isDeleted = false
            """)
    Optional<Product> findActiveUserById(@Param("id") Long id);

    /**
     * Finds a product by ID if active and not deleted.
     *
     * @param id product ID
     * @return product if found, active and not deleted
     */
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.id = :id
              AND p.isActive = true
              AND p.isDeleted = false
            """)
    Optional<Product> findByIdAndIsActiveTrueAndIsDeletedFalse(
            @Param("id") Long id
    );

    /**
     * TEMP DEBUG:
     * Returns the actual MySQL database/schema
     * currently being used by operation-service.
     */
    @Query(
            value = "SELECT DATABASE()",
            nativeQuery = true
    )
    String getCurrentDatabase();

    /**
     * TEMP DEBUG:
     * Checks directly in the products table whether
     * the supplied product ID exists.
     *
     * This bypasses JPQL/entity filtering and helps
     * confirm whether operation-service is connected
     * to the expected database.
     */
    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM products
                    WHERE id = :id
                    """,
            nativeQuery = true
    )
    Long countProductByIdNative(
            @Param("id") Long id
    );
}