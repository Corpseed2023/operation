package com.doc.repository;

import com.doc.entity.client.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    // Correct method used in createProject
    @Query("SELECT c FROM Contact c " +
            "WHERE c.id = :id " +
            "AND c.deleteStatus = false " +
            "AND c.isActive = true " +
            "AND c.isDeleted = false")
    Optional<Contact> findByIdAndDeleteStatusFalseAndIsActiveTrueAndIsDeletedFalse(@Param("id") Long id);

    // Other useful methods (recommended)
    @Query("SELECT c FROM Contact c " +
            "WHERE c.companyUnit.id = :unitId " +
            "AND c.isDeleted = false AND c.isActive = true")
    List<Contact> findByCompanyUnitIdAndIsDeletedFalseAndIsActiveTrue(@Param("unitId") Long unitId);

    @Query("SELECT c FROM Contact c " +
            "WHERE c.company.id = :companyId " +
            "AND c.companyUnit IS NULL " +
            "AND c.isDeleted = false AND c.isActive = true")
    List<Contact> findByCompanyIdAndCompanyUnitIsNullAndIsDeletedFalseAndIsActiveTrue(@Param("companyId") Long companyId);
}