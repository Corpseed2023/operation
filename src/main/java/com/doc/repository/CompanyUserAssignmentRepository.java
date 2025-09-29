package com.doc.repository;


import com.doc.entity.client.CompanyUserAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyUserAssignmentRepository extends JpaRepository<CompanyUserAssignment, Long> {

    @Query("SELECT a FROM CompanyUserAssignment a WHERE a.company.id = :companyId AND a.department.id = :departmentId AND a.isDeleted = false AND a.isActive = true")
    Optional<CompanyUserAssignment> findByCompanyIdAndDepartmentId(@Param("companyId") Long companyId,
                                                                   @Param("departmentId") Long departmentId);

    boolean existsByCompanyIdAndDepartmentIdAndIsDeletedFalseAndIsActiveTrue(Long companyId, Long departmentId);

}