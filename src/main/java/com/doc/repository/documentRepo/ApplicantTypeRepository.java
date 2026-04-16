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

    Page<ApplicantType> findByIsDeletedFalse(Pageable pageable);

    Optional<ApplicantType> findByIdAndIsActiveTrueAndIsDeletedFalse(Long id);
    Optional<ApplicantType> findByNameIgnoreCaseAndIsDeletedTrue(String name);



}