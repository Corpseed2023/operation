package com.doc.repository.documentRepo;

import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDocumentUploadRepository extends JpaRepository<ProjectDocumentUpload, Long> {

    /**
     * Finds a non-deleted document upload by ID.
     *
     * @param id the document upload ID
     * @return an Optional containing the document upload if found and not deleted
     */
    Optional<ProjectDocumentUpload> findActiveUserById(Long id);



    // ProjectDocumentUploadRepository.java
    @Query("SELECT u FROM ProjectDocumentUpload u WHERE u.project.id = :projectId AND u.isDeleted = false")
    List<ProjectDocumentUpload> findByProjectIdAndIsDeletedFalse(@Param("projectId") Long projectId);



    @Query("SELECT d FROM ProjectDocumentUpload d " +
            "WHERE d.project.id = :projectId " +
            "  AND d.requiredDocument.id = :requiredDocumentId " +
            "  AND d.isDeleted = false")
    Optional<ProjectDocumentUpload> findActiveProjectLevelDocument(
            @Param("projectId") Long projectId,
            @Param("requiredDocumentId") Long requiredDocumentId);


}