package com.doc.repository.documentRepo;

import com.doc.entity.document.ProductRequiredDocuments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRequiredDocumentRepository
        extends JpaRepository<ProductRequiredDocuments, Long> {



    Optional<ProductRequiredDocuments> findByIdAndIsDeletedFalse(Long id);

    // Existing paginated API
    Page<ProductRequiredDocuments>
    findAllByIsDeletedFalseAndIsActiveTrue(Pageable pageable);

    // NEW - fetch all active, non-deleted documents without pagination
    @Query("""
            SELECT p
            FROM ProductRequiredDocuments p
            WHERE p.isDeleted = false
              AND p.isActive = true
            ORDER BY p.name ASC
           """)
    List<ProductRequiredDocuments> findAllActiveDocuments();

    boolean existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalse(
            String name,
            String country,
            String centralName,
            String stateName
    );

    boolean existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalseAndIdNot(
            String name,
            String country,
            String centralName,
            String stateName,
            Long id
    );

    boolean existsByNameAndIsDeletedFalse(String name);



}