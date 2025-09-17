package com.doc.repository;

import com.doc.entity.product.ProductRequiredDocuments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface ProductRequiredDocumentsRepository extends JpaRepository<ProductRequiredDocuments, Long> {

    boolean existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalse(
            String name, String country, String centralName, String stateName);

    @Query("SELECT COUNT(d) > 0 FROM ProductRequiredDocuments d WHERE d.id != :id AND d.name = :name " +
            "AND d.country = :country AND d.centralName = :centralName AND d.stateName = :stateName AND d.isDeleted = false")
    boolean existsByNameAndCountryAndCentralNameAndStateNameAndIsDeletedFalseExcludingId(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("country") String country,
            @Param("centralName") String centralName,
            @Param("stateName") String stateName);

    Page<ProductRequiredDocuments> findByIsDeletedFalse(Pageable pageable);



}