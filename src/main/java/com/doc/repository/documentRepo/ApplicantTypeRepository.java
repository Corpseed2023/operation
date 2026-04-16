package com.doc.repository.documentRepo;

import com.doc.entity.document.ApplicantType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicantTypeRepository extends JpaRepository<ApplicantType, Long> {

    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIsDeletedFalseAndIdNot(String name, Long id);

    Optional<ApplicantType> findByIdAndIsDeletedFalse(Long id);

    // NEW: For pagination - only non-deleted records
    Page<ApplicantType> findByIsDeletedFalse(Pageable pageable);

    // Optional: You can keep this if you need it elsewhere
    Optional<ApplicantType> findByIdAndIsActiveTrueAndIsDeletedFalse(Long id);


}