package com.doc.repository;


import com.doc.entity.project.ProjectPaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectPaymentTransactionRepository extends JpaRepository<ProjectPaymentTransaction, Long> {

    @Query("SELECT ppt FROM ProjectPaymentTransaction ppt WHERE ppt.project.id = :projectId")
    List<ProjectPaymentTransaction> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT ppt FROM ProjectPaymentTransaction ppt WHERE ppt.project.id = :projectId")
    Page<ProjectPaymentTransaction> findByProjectId(@Param("projectId") Long projectId, Pageable pageable);

    @Query("SELECT SUM(ppt.amount) FROM ProjectPaymentTransaction ppt WHERE ppt.project.id = :projectId")
    Double findTotalPaymentsByProjectId(@Param("projectId") Long projectId);
}
