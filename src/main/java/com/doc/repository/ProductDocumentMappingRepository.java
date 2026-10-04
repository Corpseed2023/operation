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
    List<ProductDocumentMapping> findByProductIdAndApplicantTypeIdAndIsActiveTrue(
            @Param("productId") Long productId,
            @Param("applicantTypeId") Long applicantTypeId
    );

    List<ProductDocumentMapping> findByProductIdAndIsActiveTrue(
            Long productId
    );

    /*
     * Exact mapping lookup when applicant type is available.
     *
     * Unique combination:
     *
     * Product
     * + Required Document
     * + Applicant Type
     */
    @Query("""
            SELECT m
            FROM ProductDocumentMapping m
            WHERE m.product.id = :productId
              AND m.requiredDocument.id = :requiredDocumentId
              AND m.applicantType.id = :applicantTypeId
            """)
    Optional<ProductDocumentMapping> findExactMapping(
            @Param("productId") Long productId,
            @Param("requiredDocumentId") Long requiredDocumentId,
            @Param("applicantTypeId") Long applicantTypeId
    );

    /*
     * Exact mapping lookup for a global document
     * where applicant_type_id is NULL.
     */
    @Query("""
            SELECT m
            FROM ProductDocumentMapping m
            WHERE m.product.id = :productId
              AND m.requiredDocument.id = :requiredDocumentId
              AND m.applicantType IS NULL
            """)
    Optional<ProductDocumentMapping> findExactGlobalMapping(
            @Param("productId") Long productId,
            @Param("requiredDocumentId") Long requiredDocumentId
    );


    List<ProductDocumentMapping> findByProductId(Long productId);


}