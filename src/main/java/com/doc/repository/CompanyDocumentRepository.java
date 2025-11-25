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
            Long companyId, Long requiredDocumentId);

    @Query("SELECT cd FROM CompanyDocument cd " +
            "WHERE cd.company.id = :companyId " +
            "AND cd.status.name = :statusName " +
            "AND cd.isDeleted = false")
    List<CompanyDocument> findByCompanyIdAndStatusNameAndIsDeletedFalse(
            @Param("companyId") Long companyId,
            @Param("statusName") String statusName);

    @Query("SELECT cd FROM CompanyDocument cd " +
            "WHERE cd.company.id = :companyId " +
            "AND cd.requiredDocument.id = :requiredDocumentId " +
            "AND cd.status.name = :statusName " +
            "AND cd.isDeleted = false")
    Optional<CompanyDocument> findByCompanyIdAndRequiredDocumentIdAndStatusNameAndIsDeletedFalse(
            @Param("companyId") Long companyId,
            @Param("requiredDocumentId") Long requiredDocumentId,
            @Param("statusName") String statusName);

    @Query("SELECT cd FROM CompanyDocument cd WHERE cd.id = :id AND cd.isDeleted = false")
    Optional<CompanyDocument> findActiveUserById(@Param("id") Long id);


}