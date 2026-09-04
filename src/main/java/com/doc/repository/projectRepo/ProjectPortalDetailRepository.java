package com.doc.repository.projectRepo;

import com.doc.entity.project.ProjectPortalDetail;
import com.doc.entity.project.ProjectPortalDetailStatus;
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

    /**
     * Finds an active portal detail by ID and verifies that
     * it belongs to the supplied project.
     */
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
    Optional<ProjectPortalDetail> findByIdAndProjectIdAndIsDeletedFalse(
            @Param("detailId") Long detailId,
            @Param("projectId") Long projectId
    );

    /**
     * Backward-compatible/new service method.
     */
    default Optional<ProjectPortalDetail> findActiveByIdAndProjectId(
            Long detailId,
            Long projectId
    ) {
        return findByIdAndProjectIdAndIsDeletedFalse(
                detailId,
                projectId
        );
    }

    /**
     * Finds an active portal detail using only the detail ID.
     *
     * Prefer findActiveByIdAndProjectId when projectId is available.
     */
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

    /**
     * Returns all active portal details for a project.
     */
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

    /**
     * New service method pointing to the existing project query.
     */
    default List<ProjectPortalDetail> findActiveByProjectId(
            Long projectId
    ) {
        return findByProjectIdAndIsDeletedFalse(projectId);
    }

    /**
     * Retained for existing service code.
     */
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
    List<ProjectPortalDetail>
    findByProjectIdAndIsDeletedFalseOrderByCreatedDateDesc(
            @Param("projectId") Long projectId
    );

    /**
     * Checks whether an active portal with the same name
     * already exists for the project.
     */
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

    /**
     * Supports the IgnoreCase method name used previously.
     */
    default boolean
    existsByProjectIdAndPortalNameIgnoreCaseAndIsDeletedFalse(
            Long projectId,
            String portalName
    ) {
        return existsByProjectIdAndPortalNameAndIsDeletedFalse(
                projectId,
                portalName
        );
    }

    /**
     * New service method pointing to the duplicate-check query.
     */
    default boolean existsActivePortalName(
            Long projectId,
            String portalName
    ) {
        return existsByProjectIdAndPortalNameAndIsDeletedFalse(
                projectId,
                portalName
        );
    }

    /**
     * Checks duplicate portal name during update while excluding
     * the currently updated portal detail.
     */
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

    /**
     * Supports the IgnoreCase method name used previously.
     */
    default boolean
    existsByProjectIdAndPortalNameIgnoreCaseAndIsDeletedFalseAndIdNot(
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
     * New service method pointing to the update duplicate query.
     */
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
     * Returns portal details for a project filtered by status.
     */
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
              AND portal.status = :status
              AND portal.isDeleted = false
            ORDER BY portal.createdDate DESC, portal.id DESC
            """)
    List<ProjectPortalDetail>
    findByProjectIdAndStatusAndIsDeletedFalseOrderByCreatedDateDesc(
            @Param("projectId") Long projectId,
            @Param("status") ProjectPortalDetailStatus status
    );

    /**
     * New service method pointing to the status query.
     */
    default List<ProjectPortalDetail> findActiveByProjectIdAndStatus(
            Long projectId,
            ProjectPortalDetailStatus status
    ) {
        return findByProjectIdAndStatusAndIsDeletedFalseOrderByCreatedDateDesc(
                projectId,
                status
        );
    }

    /**
     * Returns all active portal details having the supplied status.
     */
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
            WHERE portal.status = :status
              AND portal.isDeleted = false
            ORDER BY portal.createdDate ASC, portal.id ASC
            """)
    List<ProjectPortalDetail> findAllActiveByStatus(
            @Param("status") ProjectPortalDetailStatus status
    );

    /**
     * Returns all pending portal details.
     */
    default List<ProjectPortalDetail> findAllPendingPortalDetails() {
        return findAllActiveByStatus(
                ProjectPortalDetailStatus.PENDING
        );
    }

    /**
     * Returns pending portal details submitted by users belonging
     * to the specified department.
     */
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
            JOIN portal.createdBy submitter
            JOIN submitter.departments department
            WHERE LOWER(TRIM(department.name))
                    = LOWER(TRIM(:departmentName))
              AND department.isDeleted = false
              AND portal.status = :status
              AND portal.isDeleted = false
            ORDER BY portal.createdDate ASC, portal.id ASC
            """)
    List<ProjectPortalDetail> findByDepartmentNameAndStatus(
            @Param("departmentName") String departmentName,
            @Param("status") ProjectPortalDetailStatus status
    );

    /**
     * Returns pending portal details for a department.
     */
    default List<ProjectPortalDetail> findPendingByDepartmentName(
            String departmentName
    ) {
        return findByDepartmentNameAndStatus(
                departmentName,
                ProjectPortalDetailStatus.PENDING
        );
    }

    /**
     * Returns pending portal details that can be reviewed by the
     * submitter's directly mapped manager.
     */
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
            JOIN portal.createdBy submitter
            JOIN submitter.departments department
            WHERE submitter.manager.id = :managerId
              AND LOWER(TRIM(department.name))
                    = LOWER(TRIM(:departmentName))
              AND department.isDeleted = false
              AND portal.status = :status
              AND portal.isDeleted = false
            ORDER BY portal.createdDate ASC, portal.id ASC
            """)
    List<ProjectPortalDetail> findForManagerByDepartmentAndStatus(
            @Param("managerId") Long managerId,
            @Param("departmentName") String departmentName,
            @Param("status") ProjectPortalDetailStatus status
    );

    /**
     * Returns pending portal details for the specified manager.
     */
    default List<ProjectPortalDetail> findPendingForManager(
            Long managerId,
            String departmentName
    ) {
        return findForManagerByDepartmentAndStatus(
                managerId,
                departmentName,
                ProjectPortalDetailStatus.PENDING
        );
    }
}