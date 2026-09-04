package com.doc.repository.projectRepo;

import com.doc.entity.project.ProjectPortalDetail;
import com.doc.entity.project.ProjectPortalDetailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectPortalDetailRepository
        extends JpaRepository<ProjectPortalDetail, Long> {

    @EntityGraph(attributePaths = {
            "project",
            "company",
            "createdBy",
            "createdBy.manager",
            "createdBy.departments",
            "updatedBy",
            "approvedBy"
    })
    @Query("""
            SELECT DISTINCT portal
            FROM ProjectPortalDetail portal
            WHERE portal.id = :detailId
              AND portal.project.id = :projectId
              AND portal.isDeleted = false
            """)
    Optional<ProjectPortalDetail>
    findByIdAndProjectIdAndIsDeletedFalse(
            @Param("detailId") Long detailId,
            @Param("projectId") Long projectId
    );

    default Optional<ProjectPortalDetail>
    findActiveByIdAndProjectId(
            Long detailId,
            Long projectId
    ) {
        return findByIdAndProjectIdAndIsDeletedFalse(
                detailId,
                projectId
        );
    }

    @EntityGraph(attributePaths = {
            "project",
            "company",
            "createdBy",
            "createdBy.manager",
            "createdBy.departments",
            "updatedBy",
            "approvedBy"
    })
    @Query("""
            SELECT DISTINCT portal
            FROM ProjectPortalDetail portal
            WHERE portal.id = :detailId
              AND portal.isDeleted = false
            """)
    Optional<ProjectPortalDetail> findByIdAndIsDeletedFalse(
            @Param("detailId") Long detailId
    );

    @EntityGraph(attributePaths = {
            "project",
            "company",
            "createdBy",
            "createdBy.manager",
            "createdBy.departments",
            "updatedBy",
            "approvedBy"
    })
    @Query("""
            SELECT DISTINCT portal
            FROM ProjectPortalDetail portal
            WHERE portal.project.id = :projectId
              AND portal.isDeleted = false
            ORDER BY portal.createdDate DESC, portal.id DESC
            """)
    List<ProjectPortalDetail> findByProjectIdAndIsDeletedFalse(
            @Param("projectId") Long projectId
    );

    default List<ProjectPortalDetail> findActiveByProjectId(
            Long projectId
    ) {
        return findByProjectIdAndIsDeletedFalse(projectId);
    }

    @Query("""
            SELECT CASE
                       WHEN COUNT(portal.id) > 0 THEN true
                       ELSE false
                   END
            FROM ProjectPortalDetail portal
            WHERE portal.project.id = :projectId
              AND LOWER(TRIM(portal.portalName))
                    = LOWER(TRIM(:portalName))
              AND portal.isDeleted = false
            """)
    boolean existsByProjectIdAndPortalNameAndIsDeletedFalse(
            @Param("projectId") Long projectId,
            @Param("portalName") String portalName
    );



    default boolean existsActivePortalName(
            Long projectId,
            String portalName
    ) {
        return existsByProjectIdAndPortalNameAndIsDeletedFalse(
                projectId,
                portalName
        );
    }

    @Query("""
            SELECT CASE
                       WHEN COUNT(portal.id) > 0 THEN true
                       ELSE false
                   END
            FROM ProjectPortalDetail portal
            WHERE portal.project.id = :projectId
              AND LOWER(TRIM(portal.portalName))
                    = LOWER(TRIM(:portalName))
              AND portal.id <> :excludedDetailId
              AND portal.isDeleted = false
            """)
    boolean existsByProjectIdAndPortalNameAndIsDeletedFalseAndIdNot(
            @Param("projectId") Long projectId,
            @Param("portalName") String portalName,
            @Param("excludedDetailId") Long excludedDetailId
    );


    default boolean existsActivePortalNameExcludingId(
            Long projectId,
            String portalName,
            Long excludedDetailId
    ) {
        return existsByProjectIdAndPortalNameAndIsDeletedFalseAndIdNot(
                projectId,
                portalName,
                excludedDetailId
        );
    }






    /**
     * Paginated query used by the approval-queue service.
     *
     * Do not fetch createdBy.departments through EntityGraph here because
     * collection fetching can cause incorrect in-memory pagination.
     */
    @EntityGraph(attributePaths = {
            "project",
            "company",
            "createdBy",
            "createdBy.manager",
            "updatedBy",
            "approvedBy"
    })
    @Query(
            value = """
                    SELECT portal
                    FROM ProjectPortalDetail portal
                    WHERE portal.status = :status
                      AND portal.isDeleted = false
                    """,
            countQuery = """
                    SELECT COUNT(portal.id)
                    FROM ProjectPortalDetail portal
                    WHERE portal.status = :status
                      AND portal.isDeleted = false
                    """
    )
    Page<ProjectPortalDetail> findAllActiveByStatus(
            @Param("status")
            ProjectPortalDetailStatus status,
            Pageable pageable
    );






    /**
     * Paginated Technical manager approval queue.
     */
    @EntityGraph(attributePaths = {
            "project",
            "company",
            "createdBy",
            "createdBy.manager",
            "updatedBy",
            "approvedBy"
    })
    @Query(
            value = """
                    SELECT DISTINCT portal
                    FROM ProjectPortalDetail portal
                    JOIN portal.createdBy submitter
                    JOIN submitter.departments department
                    WHERE submitter.manager.id = :managerId
                      AND portal.status = :status
                      AND portal.isDeleted = false
                      AND submitter.isDeleted = false
                      AND submitter.isActive = true
                      AND department.isDeleted = false
                      AND UPPER(
                            REPLACE(
                                REPLACE(
                                    TRIM(department.name),
                                    '_',
                                    ' '
                                ),
                                '-',
                                ' '
                            )
                          ) IN (
                                'TECHNICAL',
                                'TECHNICAL DEPARTMENT'
                          )
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT portal.id)
                    FROM ProjectPortalDetail portal
                    JOIN portal.createdBy submitter
                    JOIN submitter.departments department
                    WHERE submitter.manager.id = :managerId
                      AND portal.status = :status
                      AND portal.isDeleted = false
                      AND submitter.isDeleted = false
                      AND submitter.isActive = true
                      AND department.isDeleted = false
                      AND UPPER(
                            REPLACE(
                                REPLACE(
                                    TRIM(department.name),
                                    '_',
                                    ' '
                                ),
                                '-',
                                ' '
                            )
                          ) IN (
                                'TECHNICAL',
                                'TECHNICAL DEPARTMENT'
                          )
                    """
    )
    Page<ProjectPortalDetail>
    findTechnicalPortalRequestsForManager(
            @Param("managerId")
            Long managerId,
            @Param("status")
            ProjectPortalDetailStatus status,
            Pageable pageable
    );
}