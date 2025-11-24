package com.doc.repository;

import com.doc.entity.document.ApplicantType;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.product.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductDocumentMappingRepository extends JpaRepository<ProductDocumentMapping, Long> {

    void deleteByProductAndApplicantType(Product product, ApplicantType applicantType);

    List<ProductDocumentMapping> findByProductAndApplicantTypeAndIsActiveTrue(
            Product product, ApplicantType applicantType, Sort sort);
}