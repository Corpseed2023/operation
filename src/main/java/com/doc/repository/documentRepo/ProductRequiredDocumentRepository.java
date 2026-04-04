package com.doc.repository.documentRepo;

import com.doc.entity.document.ProductRequiredDocuments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRequiredDocumentRepository extends JpaRepository<ProductRequiredDocuments, Long> {

    Optional<ProductRequiredDocuments> findByIdAndIsDeletedFalse(Long id);

    Page<ProductRequiredDocuments> findAllByIsDeletedFalseAndIsActiveTrue(Pageable pageable);

    // For normal Create/Update (if you still want location-based uniqueness)
    boolean existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalse(
            String name, String country, String centralName, String stateName);

    boolean existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalseAndIdNot(
            String name, String country, String centralName, String stateName, Long id);

    // NEW: Simple name check for Import (This is what we need now)
    boolean existsByNameAndIsDeletedFalse(String name);
}