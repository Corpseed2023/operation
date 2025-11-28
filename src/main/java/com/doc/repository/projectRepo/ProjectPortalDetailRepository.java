package com.doc.repository.projectRepo;

import com.doc.entity.project.ProjectPortalDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectPortalDetailRepository extends JpaRepository<ProjectPortalDetail, Long> {

    List<ProjectPortalDetail> findByProjectIdAndIsDeletedFalse(Long projectId);

    Optional<ProjectPortalDetail> findByIdAndIsDeletedFalse(Long id);

    boolean existsByProjectIdAndPortalNameAndIsDeletedFalse(Long projectId, String portalName);

    boolean existsByIdAndProjectIdAndIsDeletedFalse(Long id, Long projectId);

    List<ProjectPortalDetail> findByProjectIdAndIsDeletedFalseOrderByCreatedDateDesc(Long projectId);
}