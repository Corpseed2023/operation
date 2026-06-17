package com.doc.repository;

import com.doc.entity.document.CompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, Long> {

    Optional<CompanyDocument> findByCompanyIdAndRequiredDocumentIdAndIsDeletedFalse(
            Long companyId,
            Long requiredDocumentId
    );

    List<CompanyDocument> findByCompanyIdAndCompanyUnitIdAndIsDeletedFalse(
            Long companyId,
            Long companyUnitId
    );

    @Query("""
        SELECT cd.requiredDocument.id
        FROM CompanyDocument cd
        WHERE cd.company.id = :companyId
        AND cd.requiredDocument.id IN :requiredDocumentIds
        AND cd.isDeleted = false
        AND cd.status.name = 'VERIFIED'
        AND (
            cd.isPermanent = true
            OR cd.expiryDate IS NULL
            OR cd.expiryDate >= CURRENT_DATE
        )
    """)
    List<Long> findReusableRequiredDocumentIdsByCompany(
            @Param("companyId") Long companyId,
            @Param("requiredDocumentIds") List<Long> requiredDocumentIds
    );



}