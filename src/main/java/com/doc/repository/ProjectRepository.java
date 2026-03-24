package com.doc.repository;

import com.doc.entity.project.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Project} entities.
 * Provides CRUD operations and custom queries for projects, ensuring soft deletion
 * and filtering by project number, company, contact, and project name.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Checks if a project with the given project number exists and is not deleted.
     *
     * @param projectNo the project number to check
     * @return true if a project with the given project number exists and is not deleted, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p WHERE p.isCancelled=false AND p.projectNo = :projectNo AND p.isDeleted = false")
    boolean existsByProjectNoAndIsDeletedFalse(@Param("projectNo") String projectNo);

    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE  p.isCancelled=false AND p.unbilledNumber = :unbilledNumber AND p.isDeleted = false")
    boolean existsByUnbilledNumberAndIsDeletedFalse(@Param("unbilledNumber") String unbilledNumber);

    /**
     * Checks if a project with the given estimate number exists and is not deleted.
     *
     * @param estimateNumber the estimate number to check
     * @return true if a project with the given estimate number exists and is not deleted, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p WHERE  p.isCancelled=false AND p.estimateNumber = :estimateNumber AND p.isDeleted = false")
    boolean existsByEstimateNumberAndIsDeletedFalse(@Param("estimateNumber") String estimateNumber);

    /**
     * Finds a project by its ID if it is not deleted.
     *
     * @param id the project ID
     * @return an {@link Optional} containing the project if found and not deleted, empty otherwise
     */
    @Query("SELECT p FROM Project p WHERE  p.isCancelled=false AND p.id = :id AND p.isDeleted = false")
    Optional<Project> findActiveUserById(@Param("id") Long id);

    /**
     * Retrieves all non-deleted projects with pagination.
     *
     * @param pageable pagination information
     * @return a {@link Page} of non-deleted projects
     */
    @Query("SELECT p FROM Project p WHERE  p.isCancelled=false AND  p.isDeleted = false")
    Page<Project> findByIsDeletedFalse(Pageable pageable);

    /**
     * Retrieves non-deleted projects assigned to specific users (based on milestone assignments), with pagination.
     *
     * @param userIds the list of user IDs
     * @param pageable pagination information
     * @return a {@link Page} of non-deleted projects assigned to the given users
     */
    @Query("SELECT DISTINCT p FROM Project p WHERE  p.isCancelled=false AND  p.isDeleted = false AND EXISTS (SELECT 1 FROM ProjectMilestoneAssignment a WHERE a.project = p AND a.assignedUser.id IN :userIds AND a.isDeleted = false)")
    Page<Project> findByAssignedUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);

    /**
     * Finds projects by product ID and milestone ID.
     *
     * @param productId the product ID
     * @param milestoneId the milestone ID
     * @return an {@link Optional} containing the project if found, empty otherwise
     */
    @Query("SELECT p FROM Project p WHERE  p.isCancelled=false AND  p.product.id = :productId AND EXISTS (SELECT 1 FROM ProjectMilestoneAssignment a WHERE a.project.id = p.id AND a.milestone.id = :milestoneId)")
    Optional<Project> findByProductIdAndMilestoneId(@Param("productId") Long productId, @Param("milestoneId") Long milestoneId);

    /**
     * Finds projects by company name (case-insensitive partial match) and not deleted.
     *
     * @param companyName the company name to search
     * @return a list of matching projects
     */
    @Query("SELECT p FROM Project p WHERE  p.isCancelled=false AND LOWER(p.company.name) LIKE LOWER(CONCAT('%', :companyName, '%')) AND p.isDeleted = false")
    List<Project> findByCompanyNameContainingAndIsDeletedFalse(@Param("companyName") String companyName);

    /**
     * Finds projects by company name (case-insensitive partial match) for specific assigned users and not deleted.
     *
     * @param companyName the company name to search
     * @param userIds the list of user IDs
     * @return a list of matching projects
     */
    @Query("SELECT DISTINCT p FROM Project p WHERE  p.isCancelled=false AND LOWER(p.company.name) LIKE LOWER(CONCAT('%', :companyName, '%')) AND p.isDeleted = false AND EXISTS (SELECT 1 FROM ProjectMilestoneAssignment a WHERE a.project = p AND a.assignedUser.id IN :userIds AND a.isDeleted = false)")
    List<Project> findByCompanyNameContainingAndAssignedUserIdsAndIsDeletedFalse(@Param("companyName") String companyName, @Param("userIds") List<Long> userIds);

    /**
     * Finds projects by project number (case-insensitive partial match) and not deleted.
     *
     * @param projectNo the project number to search
     * @return a list of matching projects
     */
    @Query("SELECT p FROM Project p WHERE  p.isCancelled=false AND  LOWER(p.projectNo) LIKE LOWER(CONCAT('%', :projectNo, '%')) AND p.isDeleted = false")
    List<Project> findByProjectNoContainingAndIsDeletedFalse(@Param("projectNo") String projectNo);

    /**
     * Finds projects by project number (case-insensitive partial match) for specific assigned users and not deleted.
     *
     * @param projectNo the project number to search
     * @param userIds the list of user IDs
     * @return a list of matching projects
     */
    @Query("SELECT DISTINCT p FROM Project p WHERE  p.isCancelled=false AND  LOWER(p.projectNo) LIKE LOWER(CONCAT('%', :projectNo, '%')) AND p.isDeleted = false AND EXISTS (SELECT 1 FROM ProjectMilestoneAssignment a WHERE a.project = p AND a.assignedUser.id IN :userIds AND a.isDeleted = false)")
    List<Project> findByProjectNoContainingAndAssignedUserIdsAndIsDeletedFalse(@Param("projectNo") String projectNo, @Param("userIds") List<Long> userIds);


    /**
     * Finds projects by contact name (case-insensitive partial match) and not deleted.
     *
     * @param contactName the contact name to search
     * @return a list of matching projects
     */
    @Query("SELECT p FROM Project p WHERE  p.isCancelled=false AND  LOWER(p.contact.name) LIKE LOWER(CONCAT('%', :contactName, '%')) AND p.isDeleted = false")
    List<Project> findByContactNameContainingAndIsDeletedFalse(@Param("contactName") String contactName);

    /**
     * Finds projects by contact name (case-insensitive partial match) for specific assigned users and not deleted.
     *
     * @param contactName the contact name to search
     * @param userIds the list of user IDs
     * @return a list of matching projects
     */
    @Query("SELECT DISTINCT p FROM Project p WHERE  p.isCancelled=false AND LOWER(p.contact.name) LIKE LOWER(CONCAT('%', :contactName, '%')) AND p.isDeleted = false AND EXISTS (SELECT 1 FROM ProjectMilestoneAssignment a WHERE a.project = p AND a.assignedUser.id IN :userIds AND a.isDeleted = false)")
    List<Project> findByContactNameContainingAndAssignedUserIdsAndIsDeletedFalse(@Param("contactName") String contactName, @Param("userIds") List<Long> userIds);

    /**
     * Finds projects by project name (case-insensitive partial match) and not deleted.
     *
     * @param name the project name to search
     * @return a list of matching projects
     */
    @Query("SELECT p FROM Project p WHERE  p.isCancelled=false AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isDeleted = false")
    List<Project> findByNameContainingAndIsDeletedFalse(@Param("name") String name);

    /**
     * Finds projects by project name (case-insensitive partial match) for specific assigned users and not deleted.
     *
     * @param name the project name to search
     * @param userIds the list of user IDs
     * @return a list of matching projects
     */
    @Query("SELECT DISTINCT p FROM Project p WHERE  p.isCancelled=false AND  LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isDeleted = false AND EXISTS (SELECT 1 FROM ProjectMilestoneAssignment a WHERE a.project = p AND a.assignedUser.id IN :userIds AND a.isDeleted = false)")
    List<Project> findByNameContainingAndAssignedUserIdsAndIsDeletedFalse(@Param("name") String name, @Param("userIds") List<Long> userIds);

    @Query("SELECT p FROM Project p WHERE  p.isCancelled=false AND p.unbilledNumber = :unbilledNumber AND p.isDeleted = false")
    Optional<Project> findByUnbilledNumberAndIsDeletedFalse(@Param("unbilledNumber") String unbilledNumber);

    /**
     * Counts all non-deleted projects (for admins).
     *
     * @return total number of active projects
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE  p.isCancelled=false AND  p.isDeleted = false")
    long countByIsDeletedFalse();

    /**
     * Counts non-deleted projects assigned to specific users (based on milestone assignments).
     * Used for managers and regular users to get total accessible project count.
     *
     * @param userIds the list of user IDs (including subordinates if manager)
     * @return total number of accessible projects
     */
    @Query("SELECT COUNT(DISTINCT p) FROM Project p WHERE  p.isCancelled=false AND  p.isDeleted = false AND EXISTS (SELECT 1 FROM ProjectMilestoneAssignment a WHERE a.project = p AND a.assignedUser.id IN :userIds AND a.isDeleted = false)")
    long countByAssignedUserIds(@Param("userIds") List<Long> userIds);


    // ADD THESE EXACT METHODS TO ProjectRepository.java

    @Query("SELECT p FROM Project p " +
            "LEFT JOIN FETCH p.applicantType " +
            "LEFT JOIN FETCH p.product " +
            "WHERE  p.isCancelled=false AND p.id = :id AND p.isDeleted = false")
    Optional<Project> findByIdWithApplicantTypeAndProduct(@Param("id") Long id);

    @Query("SELECT p FROM Project p " +
            "WHERE  p.isCancelled=false AND  p.id = :id AND p.isDeleted = false")
    Optional<Project> findByIdAndIsDeletedFalse(@Param("id") Long id);

    @Query("SELECT p FROM Project p " +
            "LEFT JOIN FETCH p.applicantType " +
            "WHERE  p.isCancelled=false AND p.id = :id AND p.isDeleted = false")
    Optional<Project> findByIdWithApplicantType(@Param("id") Long id);



    long countByStatus_NameAndIsDeletedFalseAndIsCancelledFalse(String statusName);

    long countBySalesPersonIdAndIsDeletedFalseAndIsCancelledFalse(Long salesPersonId);

    long countBySalesPersonIdAndStatus_NameAndIsDeletedFalseAndIsCancelledFalse(Long salesPersonId, String statusName);

}