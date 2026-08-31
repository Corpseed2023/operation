package com.doc.repository.research;

import com.doc.entity.research.TechnicalResearchCase;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TechnicalResearchCaseRepository
        extends JpaRepository<TechnicalResearchCase, Long>,
        JpaSpecificationExecutor<TechnicalResearchCase> {

    Optional<TechnicalResearchCase> findByIdAndDeletedFalse(Long id);

    boolean existsByCaseNumber(String caseNumber);

    /**
     * Loads and locks the case during assignment.
     *
     * This prevents simultaneous assignment by multiple managers.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT researchCase
            FROM TechnicalResearchCase researchCase
            WHERE researchCase.id = :caseId
              AND researchCase.deleted = false
            """)
    Optional<TechnicalResearchCase> findByIdForAssignment(
            @Param("caseId") Long caseId
    );

    /**
     * Returns cases raised by or currently assigned to the user.
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


    @EntityGraph(attributePaths = {
            "product",
            "raisedBy",
            "currentAssignee",
            "lastAssignedBy",
            "closedBy"
    })
    Page<TechnicalResearchCase>
    findByOriginatingLeadIdAndDeletedFalse(
            Long originatingLeadId,
            Pageable pageable
    );


}