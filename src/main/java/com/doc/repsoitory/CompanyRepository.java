package com.doc.repsoitory;

import com.doc.entity.client.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByGstNoAndIsDeletedFalse(String gstNo);

    Page<Company> findByIsDeletedFalse(Pageable pageable);
}