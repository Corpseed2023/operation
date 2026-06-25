package com.doc.repository;

import com.doc.em.ProjectReopenRequestStatus;
import com.doc.entity.project.ProjectReopenRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectReopenRequestRepository extends JpaRepository<ProjectReopenRequest, Long> {

    Optional<ProjectReopenRequest> findByIdAndIsDeletedFalse(Long id);

    boolean existsByProjectIdAndStatusInAndIsDeletedFalse(
            Long projectId,
            Collection<ProjectReopenRequestStatus> statuses
    );

    List<ProjectReopenRequest> findByRequesterManagerIdAndStatusAndIsDeletedFalse(
            Long managerId,
            ProjectReopenRequestStatus status
    );

    List<ProjectReopenRequest> findByResponsibleManagerIdAndStatusAndIsDeletedFalse(
            Long managerId,
            ProjectReopenRequestStatus status
    );

    List<ProjectReopenRequest> findByProjectIdAndIsDeletedFalseOrderByCreatedDateDesc(Long projectId);
}