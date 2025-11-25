package com.doc.repository;

import com.doc.entity.document.ApplicantType;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.product.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ProductDocumentMappingRepository extends JpaRepository<ProductDocumentMapping, Long> {

    @Modifying
    @Query("DELETE FROM ProductDocumentMapping m WHERE m.product.id = :productId " +
            "AND (m.applicantType.id = :applicantTypeId OR (:applicantTypeId IS NULL AND m.applicantType IS NULL))")
    void deleteByProductIdAndApplicantTypeId(@Param("productId") Long productId,
                                             @Param("applicantTypeId") Long applicantTypeId);

    List<ProductDocumentMapping> findByProductIdAndApplicantTypeIdAndIsActiveTrue(Long productId, Long applicantTypeId);

    List<ProductDocumentMapping> findByProductIdAndApplicantTypeIsNullAndIsActiveTrue(Long productId);

    List<ProductDocumentMapping> findByProductIdAndApplicantTypeIsNotNullAndIsActiveTrue(Long productId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
            "FROM ProductDocumentMapping m " +
            "WHERE m.product.id = :productId " +
            "AND m.applicantType IS NOT NULL " +
            "AND m.isActive = true")
    boolean existsByProductIdAndApplicantTypeIsNotNullAndIsActiveTrue(@Param("productId") Long productId);




}