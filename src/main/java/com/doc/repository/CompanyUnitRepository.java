package com.doc.repository;

import com.doc.entity.client.Company;
import com.doc.entity.client.CompanyUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyUnitRepository extends JpaRepository<CompanyUnit, Long> {

    Optional<CompanyUnit> findByIdAndCompanyId(Long unitId, Long id);

    Optional<CompanyUnit> findByIdAndCompanyIdAndIsDeletedFalse(Long unitId, Long id);
}