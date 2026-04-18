package com.doc.repository;

import com.doc.entity.legalrequest.LegalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LegalRequestRepository extends JpaRepository<LegalRequest, Long>,
        JpaSpecificationExecutor<LegalRequest> {

    Page<LegalRequest> findByAssignedTo(Long assignedTo, Pageable pageable);

    /**
     * Optional: Custom query if you need more control later
     */
    @Query("SELECT lr FROM LegalRequest lr " +
            "LEFT JOIN FETCH lr.project " +
            "LEFT JOIN FETCH lr.projectMilestoneAssignment pma " +
            "LEFT JOIN FETCH pma.milestone " +
            "WHERE lr.id = :id")
    Optional<LegalRequest> findByIdWithDetails(@Param("id") Long id);
}