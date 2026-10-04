package com.doc.repository;

import com.doc.entity.document.ApplicantType;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductDocumentMappingRepository
        extends JpaRepository<ProductDocumentMapping, Long> {

    @Query("""
            SELECT m
            FROM ProductDocumentMapping m
            WHERE m.product = :product
              AND m.applicantType = :applicantType
              AND m.isActive = true
            """)
    List<ProductDocumentMapping> findByProductAndApplicantType(
            @Param("product") Product product,
            @Param("applicantType") ApplicantType applicantType
    );

    @Query("""
            SELECT m
            FROM ProductDocumentMapping m
            WHERE m.product.id = :productId
              AND m.applicantType.id = :applicantTypeId
              AND m.isActive = true
            """)
    List<ProductDocumentMapping>
    findByProductIdAndApplicantTypeIdAndIsActiveTrue(
            @Param("productId") Long productId,
            @Param("applicantTypeId") Long applicantTypeId
    );

    List<ProductDocumentMapping>
    findByProductIdAndIsActiveTrue(
            Long productId
    );

    /*
     * IMPORTANT:
     *
     * Includes active + inactive mappings.
     * Required by update/reactivation flow.
     */
    List<ProductDocumentMapping>
    findByProductId(
            Long productId
    );

    /*
     * Exact applicant-specific combination.
     */
    @Query("""
            SELECT m
            FROM ProductDocumentMapping m
            WHERE m.product.id = :productId
              AND m.requiredDocument.id = :documentId
              AND m.applicantType.id = :applicantTypeId
            """)
    Optional<ProductDocumentMapping> findExactMapping(
            @Param("productId") Long productId,
            @Param("documentId") Long documentId,
            @Param("applicantTypeId") Long applicantTypeId
    );

    /*
     * Exact GLOBAL combination.
     *
     * applicantType IS NULL.
     */
    @Query("""
            SELECT m
            FROM ProductDocumentMapping m
            WHERE m.product.id = :productId
              AND m.requiredDocument.id = :documentId
              AND m.applicantType IS NULL
            """)
    Optional<ProductDocumentMapping> findExactGlobalMapping(
            @Param("productId") Long productId,
            @Param("documentId") Long documentId
    );


    @Query("""
        SELECT m
        FROM ProductDocumentMapping m
        WHERE m.product.id = :productId
          AND m.isActive = true
          AND (
                m.applicantType IS NULL
                OR m.applicantType.id = :applicantTypeId
              )
        """)
    List<ProductDocumentMapping>
    findActiveByProductIdAndApplicantTypeIncludingGlobal(
            @Param("productId") Long productId,
            @Param("applicantTypeId") Long applicantTypeId
    );



}