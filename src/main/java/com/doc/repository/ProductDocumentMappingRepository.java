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


    List<ProductDocumentMapping> findByProductIdAndApplicantTypeIdAndIsActiveTrue(Long productId, Long applicantTypeId);

    List<ProductDocumentMapping> findByProductIdAndApplicantTypeIsNullAndIsActiveTrue(Long productId);

    List<ProductDocumentMapping> findByProductIdAndApplicantTypeIsNotNullAndIsActiveTrue(Long productId);

    /** All active mappings for a product (regardless of applicant type) */
    List<ProductDocumentMapping> findByProductIdAndIsActiveTrue(Long productId);


}