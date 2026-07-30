package com.doc.repository.projectRepo;

import com.doc.entity.project.ProjectLifecycleAction;
import com.doc.entity.project.ProjectLifecycleRequest;
import com.doc.entity.project.ProjectLifecycleRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectLifecycleRequestRepository
        extends JpaRepository<ProjectLifecycleRequest, Long> {

    boolean existsByProjectIdAndRequestStatusAndDeletedFalse(
            Long projectId,
            ProjectLifecycleRequestStatus requestStatus
    );

    boolean existsByProjectIdAndActionTypeAndRequestStatusAndDeletedFalse(
            Long projectId,
            ProjectLifecycleAction actionType,
            ProjectLifecycleRequestStatus requestStatus
    );

    Page<ProjectLifecycleRequest>
    findByRequestStatusAndDeletedFalseOrderByRequestedAtDesc(
            ProjectLifecycleRequestStatus requestStatus,
            Pageable pageable
    );

    Page<ProjectLifecycleRequest>
    findByRequestedByIdAndDeletedFalseOrderByRequestedAtDesc(
            Long requestedById,
            Pageable pageable
    );

    List<ProjectLifecycleRequest>
    findByProjectIdAndDeletedFalseOrderByRequestedAtDesc(
            Long projectId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT request
            FROM ProjectLifecycleRequest request
            JOIN FETCH request.project project
            JOIN FETCH request.requestedBy requestedBy
            LEFT JOIN FETCH request.reviewedBy reviewedBy
            LEFT JOIN FETCH request.previousProjectStatus previousStatus
            WHERE request.id = :requestId
              AND request.deleted = false
            """)
    Optional<ProjectLifecycleRequest> findByIdForReview(
            @Param("requestId") Long requestId
    );
}