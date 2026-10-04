package com.doc.repository;

import com.doc.entity.milestone.Milestone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
/**
 * Repository interface for managing {@link Milestone} entities.
 */
@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    /**
     * Checks if a milestone with the given name exists.
     *
     * @param name the milestone name
     * @return true if a milestone with the name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Finds milestones by department ID with pagination.
     *
     * @param departmentId the department ID
     * @param pageable pagination information
     * @return a page of milestones associated with the department
     */
    Page<Milestone> findByDepartmentsId(Long departmentId, Pageable pageable);



    @Query(value = "SELECT * FROM milestones WHERE name = :name LIMIT 1",
            nativeQuery = true)
    Optional<Milestone> findAnyByName(@Param("name") String name);
}