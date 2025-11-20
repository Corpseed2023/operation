package com.doc.repository;

import com.doc.entity.document.ApplicantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicantTypeRepository extends JpaRepository<ApplicantType, Long> {

    List<ApplicantType> findAllByIsActiveTrueOrderByName();

    Optional<ApplicantType> findByIdAndIsActiveTrue(Long id);

    boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndIsActiveTrue(String name, Long id);
}