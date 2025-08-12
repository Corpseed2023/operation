package com.doc.repsoitory;

import com.doc.entity.product.ProductRequiredDocuments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRequiredDocumentsRepository extends JpaRepository<ProductRequiredDocuments, Long> {

    boolean existsByNameAndIsDeletedFalse(String name);

    Page<ProductRequiredDocuments> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT d FROM ProductRequiredDocuments d WHERE " +
            "(:name IS NULL OR d.name LIKE %:name%) AND " +
            "(:type IS NULL OR d.type = :type) AND " +
            "(:country IS NULL OR d.country = :country) AND " +
            "(:centralName IS NULL OR d.centralName = :centralName) AND " +
            "(:stateName IS NULL OR d.stateName = :stateName) AND " +
            "d.isDeleted = false")
    Page<ProductRequiredDocuments> findByFilters(
            @Param("name") String name,
            @Param("type") String type,
            @Param("country") String country,
            @Param("centralName") String centralName,
            @Param("stateName") String stateName,
            Pageable pageable);


}