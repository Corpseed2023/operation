package com.doc.repository;

import com.doc.entity.project.ProjectDocumentUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectDocumentUploadRepository extends JpaRepository<ProjectDocumentUpload, UUID> {

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
     * @param id the document upload UUID
     * @return an Optional containing the document upload if found and not deleted
     */
    Optional<ProjectDocumentUpload> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Checks if a non-deleted document upload exists for the given project, milestone assignment, and required document.
     *
     * @param projectId the project ID
     * @param milestoneAssignmentId the milestone assignment ID
     * @param requiredDocumentUuid the required document UUID
     * @return true if a matching non-deleted upload exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM ProjectDocumentUpload d " +
            "WHERE d.project.id = :projectId AND d.milestoneAssignment.id = :milestoneAssignmentId " +
            "AND d.requiredDocument.uuid = :requiredDocumentUuid AND d.isDeleted = false")
    boolean existsByProjectIdAndMilestoneAssignmentIdAndRequiredDocumentUuidAndIsDeletedFalse(
            @Param("projectId") Long projectId,
            @Param("milestoneAssignmentId") Long milestoneAssignmentId,
            @Param("requiredDocumentUuid") UUID requiredDocumentUuid);

    /**
     * Finds a non-deleted document upload by project ID, milestone assignment ID, and required document UUID.
     *
     * @param projectId the project ID
     * @param milestoneAssignmentId the milestone assignment ID
     * @param requiredDocumentUuid the required document UUID
     * @return an Optional containing the document upload if found and not deleted
     */
    Optional<ProjectDocumentUpload> findByProjectIdAndMilestoneAssignmentIdAndRequiredDocumentUuidAndIsDeletedFalse(
            Long projectId, Long milestoneAssignmentId, UUID requiredDocumentUuid);
}