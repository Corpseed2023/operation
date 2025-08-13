package com.doc.repository;

import com.doc.entity.project.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing {@link Project} entities.
 * Provides CRUD operations and custom queries for projects, ensuring soft deletion
 * and filtering by project number, company, and product.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Checks if a project with the given project number exists and is not deleted.
     *
     * @param projectNo the project number to check
     * @return true if a project with the given project number exists and is not deleted, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p WHERE p.projectNo = :projectNo AND p.isDeleted = false")
    boolean existsByProjectNoAndIsDeletedFalse(@Param("projectNo") String projectNo);

    /**
     * Finds a project by its ID if it is not deleted.
     *
     * @param id the project ID
     * @return an {@link Optional} containing the project if found and not deleted, empty otherwise
     */
    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Project> findByIdAndIsDeletedFalse(@Param("id") Long id);

    /**
     * Retrieves all non-deleted projects with pagination.
     *
     * @param pageable pagination information
     * @return a {@link Page} of non-deleted projects
     */
    @Query("SELECT p FROM Project p WHERE p.isDeleted = false")
    Page<Project> findByIsDeletedFalse(Pageable pageable);

    /**
     * Retrieves non-deleted projects associated with a specific company, with pagination.
     *
     * @param companyId the ID of the company
     * @param pageable pagination information
     * @return a {@link Page} of non-deleted projects for the given company
     */
    @Query("SELECT p FROM Project p WHERE p.company.id = :companyId AND p.isDeleted = false")
    Page<Project> findByCompanyIdAndIsDeletedFalse(@Param("companyId") Long companyId, Pageable pageable);

    /**
     * Retrieves non-deleted projects associated with a specific product, with pagination.
     *
     * @param productId the ID of the product
     * @param pageable pagination information
     * @return a {@link Page} of non-deleted projects for the given product
     */
    @Query("SELECT p FROM Project p WHERE p.product.id = :productId AND p.isDeleted = false")
    Page<Project> findByProductIdAndIsDeletedFalse(@Param("productId") Long productId, Pageable pageable);
}
