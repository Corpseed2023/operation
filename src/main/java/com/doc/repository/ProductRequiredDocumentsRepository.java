package com.doc.repository;

import com.doc.entity.product.ProductRequiredDocuments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

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

    @Query("SELECT d FROM ProductRequiredDocuments d WHERE d.isDeleted = false " +
            "AND (:name IS NULL OR d.name LIKE %:name%) " +
            "AND (:type IS NULL OR d.type = :type) " +
            "AND (:country IS NULL OR d.country = :country) " +
            "AND (:centralName IS NULL OR d.centralName = :centralName) " +
            "AND (:stateName IS NULL OR d.stateName = :stateName)")
    Page<ProductRequiredDocuments> findByFilters(
            @Param("name") String name,
            @Param("type") String type,
            @Param("country") String country,
            @Param("centralName") String centralName,
            @Param("stateName") String stateName,
            Pageable pageable);

    @Query("SELECT d FROM ProductRequiredDocuments d WHERE d.isDeleted = false " +
            "AND (d.createdBy = :userId OR d.updatedBy = :userId) " +
            "AND (:name IS NULL OR d.name LIKE %:name%) " +
            "AND (:type IS NULL OR d.type = :type) " +
            "AND (:country IS NULL OR d.country = :country) " +
            "AND (:centralName IS NULL OR d.centralName = :centralName) " +
            "AND (:stateName IS NULL OR d.stateName = :stateName)")
    Page<ProductRequiredDocuments> findByFiltersAndUserId(
            @Param("name") String name,
            @Param("type") String type,
            @Param("country") String country,
            @Param("centralName") String centralName,
            @Param("stateName") String stateName,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("SELECT d FROM ProductRequiredDocuments d WHERE d.isDeleted = false " +
            "AND (d.createdBy = :userId OR d.updatedBy = :userId)")
    Page<ProductRequiredDocuments> findByCreatedByOrUpdatedByAndIsDeletedFalse(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("SELECT d FROM ProductRequiredDocuments d WHERE d.uuid = :uuid AND d.isDeleted = false")
    Optional<ProductRequiredDocuments> findByUuidAndIsDeletedFalse(@Param("uuid") UUID uuid);
}