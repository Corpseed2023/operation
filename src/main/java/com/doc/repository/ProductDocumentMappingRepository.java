package com.doc.repository;

import com.doc.entity.document.ApplicantType;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductDocumentMappingRepository extends JpaRepository<ProductDocumentMapping, Long> {

//
//    List<ProductDocumentMapping> findByProductIdAndApplicantTypeIdAndIsActiveTrue(Long productId, Long applicantTypeId);

    @Query("SELECT m FROM ProductDocumentMapping m " +
            "WHERE m.product = :product " +
            "AND m.applicantType = :applicantType " +
            "AND m.isActive = true")
    List<ProductDocumentMapping> findByProductAndApplicantType(
            @Param("product") Product product,
            @Param("applicantType") ApplicantType applicantType);

    /**
     * Alternative method using IDs (you can keep both)
     */
    @Query("SELECT m FROM ProductDocumentMapping m " +
            "WHERE m.product.id = :productId " +
            "AND m.applicantType.id = :applicantTypeId " +
            "AND m.isActive = true")
    List<ProductDocumentMapping> findByProductIdAndApplicantTypeIdAndIsActiveTrue(
            @Param("productId") Long productId,
            @Param("applicantTypeId") Long applicantTypeId);

    List<ProductDocumentMapping> findByProductIdAndIsActiveTrue(Long productId);

}