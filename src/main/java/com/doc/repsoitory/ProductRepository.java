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

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByProductNameAndIsDeletedFalse(String productName);

    Page<Product> findByIsDeletedFalse(Pageable pageable);

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

    // Corrected method to find products by required document ID
    @Query("SELECT p FROM Product p JOIN p.requiredDocuments d WHERE d.id = :documentId AND p.isDeleted = false")
    List<Product> findByRequiredDocumentsId(@Param("documentId") Long documentId);
}