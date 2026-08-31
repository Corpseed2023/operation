package com.doc.repository.research;

import com.doc.entity.research.TechnicalResearchCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TechnicalResearchCaseRepository
        extends JpaRepository<TechnicalResearchCase, Long>,
        JpaSpecificationExecutor<TechnicalResearchCase> {

    Optional<TechnicalResearchCase> findByIdAndDeletedFalse(Long id);

    boolean existsByCaseNumber(String caseNumber);

    /**
     * Returns cases where the user:
     *
     * 1. Raised the request as a salesperson, or
     * 2. Is currently assigned as the technical person.
     */
    @EntityGraph(attributePaths = {
            "product",
            "raisedBy",
            "currentAssignee",
            "lastAssignedBy",
            "closedBy"
    })
    @Query("""
            SELECT researchCase
            FROM TechnicalResearchCase researchCase
            WHERE researchCase.deleted = false
              AND (
                    researchCase.raisedBy.id = :userId
                    OR researchCase.currentAssignee.id = :userId
              )
            """)
    Page<TechnicalResearchCase> findCasesForUser(
            @Param("userId") Long userId,
            Pageable pageable
    );
}