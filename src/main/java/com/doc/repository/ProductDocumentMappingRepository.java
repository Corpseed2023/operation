package com.doc.repository;

import com.doc.entity.document.ProductDocumentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
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