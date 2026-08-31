package com.doc.repository.research;

import com.doc.entity.research.TechnicalResearchCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TechnicalResearchCaseRepository
        extends JpaRepository<TechnicalResearchCase, Long>,
        JpaSpecificationExecutor<TechnicalResearchCase> {

    Optional<TechnicalResearchCase> findByIdAndDeletedFalse(Long id);

    boolean existsByCaseNumber(String caseNumber);
}