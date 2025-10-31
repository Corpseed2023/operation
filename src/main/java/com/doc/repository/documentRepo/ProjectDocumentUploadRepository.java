package com.doc.repository.documentRepo;

import com.doc.entity.document.ProjectDocumentUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDocumentUploadRepository extends JpaRepository<ProjectDocumentUpload, Long> {

    /**
     * Finds all non-deleted document uploads by milestone assignment ID.
     *
     * @param milestoneAssignmentId the milestone assignment ID
     * @return a list of non-deleted document uploads
     */
    List<ProjectDocumentUpload> findByMilestoneAssignmentIdAndIsDeletedFalse(Long milestoneAssignmentId);

    /**
     * Finds a non-deleted document upload by ID.
     *
     * @param id the document upload ID
     * @return an Optional containing the document upload if found and not deleted
     */
    Optional<ProjectDocumentUpload> findActiveUserById(Long id);

    /**
     * Checks if a non-deleted document upload exists for the given project, milestone assignment, and required document.
     *
     * @param projectId the project ID
     * @param milestoneAssignmentId the milestone assignment ID
     * @param requiredDocumentId the required document ID
     * @return true if a matching non-deleted upload exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM ProjectDocumentUpload d " +
            "WHERE d.project.id = :projectId AND d.milestoneAssignment.id = :milestoneAssignmentId " +
            "AND d.requiredDocument.id = :requiredDocumentId AND d.isDeleted = false")
    boolean existsByProjectIdAndMilestoneAssignmentIdAndRequiredDocumentIdAndIsDeletedFalse(
            @Param("projectId") Long projectId,
            @Param("milestoneAssignmentId") Long milestoneAssignmentId,
            @Param("requiredDocumentId") Long requiredDocumentId);

    /**
     * Finds a non-deleted document upload by project ID, milestone assignment ID, and required document ID.
     *
     * @param projectId the project ID
     * @param milestoneAssignmentId the milestone assignment ID
     * @param requiredDocumentId the required document ID
     * @return an Optional containing the document upload if found and not deleted
     */
    Optional<ProjectDocumentUpload> findByProjectIdAndMilestoneAssignmentIdAndRequiredDocumentIdAndIsDeletedFalse(
            Long projectId, Long milestoneAssignmentId, Long requiredDocumentId);

    @Query("SELECT d FROM ProjectDocumentUpload d WHERE d.project.id = :projectId AND d.requiredDocument.id = :requiredDocumentId AND d.isDeleted = false")
    List<ProjectDocumentUpload> findByProjectIdAndRequiredDocumentIdAndIsDeletedFalse(@Param("projectId") Long projectId, @Param("requiredDocumentId") Long requiredDocumentId);
}