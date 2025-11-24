// src/main/java/com/doc/repository/documentRepo/ProductRequiredDocumentRepository.java
package com.doc.repository.documentRepo;

import com.doc.entity.document.ProductRequiredDocuments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRequiredDocumentRepository extends JpaRepository<ProductRequiredDocuments, Long> {

    boolean existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalse(
            String name, String country, String centralName, String stateName);

    boolean existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalseAndIdNot(
            String name, String country, String centralName, String stateName, Long id);

    Optional<ProductRequiredDocuments> findByIdAndIsDeletedFalse(Long id);

    Page<ProductRequiredDocuments> findAllByIsDeletedFalse(Pageable pageable);

    @Query("SELECT prd FROM ProductRequiredDocuments prd WHERE prd.isDeleted = false AND prd.isActive = true")
    Page<ProductRequiredDocuments> findAllActive(Pageable pageable);
}