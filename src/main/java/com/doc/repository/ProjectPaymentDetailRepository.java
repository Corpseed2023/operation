package com.doc.repository;

import com.doc.entity.project.ProjectPaymentDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectPaymentDetailRepository extends JpaRepository<ProjectPaymentDetail, Long> {

    @Query("SELECT ppd FROM ProjectPaymentDetail ppd WHERE ppd.project.id = :projectId AND ppd.isDeleted = false")
    Optional<ProjectPaymentDetail> findByProjectIdAndIsDeletedFalse(@Param("projectId") Long projectId);

    @Query("SELECT ppd FROM ProjectPaymentDetail ppd WHERE ppd.paymentStatus = :status AND ppd.isDeleted = false")
    Page<ProjectPaymentDetail> findByPaymentStatusAndIsDeletedFalse(@Param("status") String status, Pageable pageable);
}
