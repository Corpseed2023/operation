package com.doc.repository.documentRepo;

import com.doc.entity.document.ProjectDocumentUpload;
import com.doc.entity.project.Project;
import com.doc.repository.ProjectPendingDocumentProjection;
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

    @Query("SELECT u FROM ProjectDocumentUpload u WHERE u.project.id = :projectId AND u.isDeleted = false")
    List<ProjectDocumentUpload> findByProjectIdAndIsDeletedFalse(@Param("projectId") Long projectId);


    @Query("SELECT d FROM ProjectDocumentUpload d " +
            "WHERE d.project.id = :projectId " +
            "  AND d.requiredDocument.id = :requiredDocumentId " +
            "  AND d.isDeleted = false")
    Optional<ProjectDocumentUpload> findActiveProjectLevelDocument(
            @Param("projectId") Long projectId,
            @Param("requiredDocumentId") Long requiredDocumentId);

    @Query(
            value = """
                    SELECT
                        p.id AS projectId,

                        COUNT(
                            DISTINCT CASE
                                WHEN pdm.is_active = 1
                                 AND pdm.is_mandatory = 1
                                 AND prd.is_active = 1
                                 AND prd.is_deleted = 0
                                THEN pdm.required_document_id
                            END
                        ) AS totalRequiredDocuments,

                        COUNT(
                            DISTINCT CASE
                                WHEN pdm.is_active = 1
                                 AND pdm.is_mandatory = 1
                                 AND prd.is_active = 1
                                 AND prd.is_deleted = 0
                                 AND pdu.id IS NOT NULL
                                THEN pdm.required_document_id
                            END
                        ) AS uploadedDocuments,

                        COUNT(
                            DISTINCT CASE
                                WHEN pdm.is_active = 1
                                 AND pdm.is_mandatory = 1
                                 AND prd.is_active = 1
                                 AND prd.is_deleted = 0
                                THEN pdm.required_document_id
                            END
                        )
                        -
                        COUNT(
                            DISTINCT CASE
                                WHEN pdm.is_active = 1
                                 AND pdm.is_mandatory = 1
                                 AND prd.is_active = 1
                                 AND prd.is_deleted = 0
                                 AND pdu.id IS NOT NULL
                                THEN pdm.required_document_id
                            END
                        ) AS pendingDocuments

                    FROM project p

                    LEFT JOIN product_document_mapping pdm
                           ON pdm.product_id = p.product_id

                    LEFT JOIN product_required_documents prd
                           ON prd.id = pdm.required_document_id

                    LEFT JOIN project_document_upload pdu
                           ON pdu.project_id = p.id
                          AND pdu.required_document_id =
                              pdm.required_document_id
                          AND pdu.is_deleted = 0
                          AND pdu.is_expired = 0
                          AND pdu.validation_passed = 1

                    WHERE p.id IN (:projectIds)

                    GROUP BY p.id
                    """,
            nativeQuery = true
    )
    List<ProjectPendingDocumentProjection> findPendingDocumentCounts(
            @Param("projectIds") List<Long> projectIds
    );


}