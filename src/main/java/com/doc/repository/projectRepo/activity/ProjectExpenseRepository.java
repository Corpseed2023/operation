package com.doc.repository.projectRepo.activity;

import com.doc.em.ApprovalStatus;
import com.doc.entity.project.activity.ProjectExpense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectExpenseRepository extends JpaRepository<ProjectExpense, Long> {

    Optional<ProjectExpense> findByActivityId(Long activityId);

    @Query("SELECT e FROM ProjectExpense e WHERE e.project.id = :projectId ORDER BY e.expenseDate DESC")
    List<ProjectExpense> findByProjectIdOrderByExpenseDateDesc(@Param("projectId") Long projectId);


    List<ProjectExpense> findAllByOrderByExpenseDateDesc();

    // Fetch expenses by approval status ordered by date descending
    List<ProjectExpense> findByApprovalStatusOrderByExpenseDateDesc(ApprovalStatus status);
}