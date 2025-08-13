package com.doc.repository;

import com.doc.entity.client.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByGstNoAndIsDeletedFalse(String gstNo);

    Page<Company> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT c FROM Company c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Company> findByIdAndIsDeletedFalse(@Param("id") Long id);
}