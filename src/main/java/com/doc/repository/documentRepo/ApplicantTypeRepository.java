package com.doc.repository.documentRepo;

import com.doc.entity.document.ApplicantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicantTypeRepository extends JpaRepository<ApplicantType, Long> {

    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIsDeletedFalseAndIdNot(String name, Long id);

    Optional<ApplicantType> findByIdAndIsDeletedFalse(Long id);

    Optional<ApplicantType> findByIdAndIsActiveTrueAndIsDeletedFalse(Long id);




}