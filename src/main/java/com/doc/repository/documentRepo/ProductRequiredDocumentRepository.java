// src/main/java/com/doc/repository/documentRepo/ProductRequiredDocumentRepository.java
package com.doc.repository.documentRepo;

import com.doc.entity.document.ApplicantType;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRequiredDocumentRepository extends JpaRepository<ProductRequiredDocuments, Long> {

    Optional<ProductRequiredDocuments> findByIdAndIsDeletedFalse(Long id);


    Page<ProductRequiredDocuments> findAllByIsDeletedFalseAndIsActiveTrue(Pageable pageable);

    boolean existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalse(
            String name, String country, String centralName, String stateName);

    boolean existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalseAndIdNot(
            String name, String country, String centralName, String stateName, Long id);

}