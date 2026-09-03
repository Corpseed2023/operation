package com.doc.repository.projectRepo.activity;

import com.doc.em.ApprovalStatus;
import com.doc.em.ExpenseApprovalStage;
import com.doc.em.ExpensePaymentStatus;
import com.doc.entity.project.activity.ProjectExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectExpenseRepository
        extends JpaRepository<ProjectExpense, Long> {
    //

    Optional<ProjectExpense> findByActivityId(Long activityId);

    List<ProjectExpense> findByProjectIdOrderByExpenseDateDesc(
            Long projectId
    );

    // =========================================================
    // GENERIC STAGE QUERY
    // =========================================================

    List<ProjectExpense> findByApprovalStageOrderByExpenseDateDesc(
            ExpenseApprovalStage approvalStage
    );

    // =========================================================
    // CRT APPROVAL QUEUE
    // =========================================================

    List<ProjectExpense>
    findByApprovalStageAndCrtApprovalStatusOrderByExpenseDateDesc(
            ExpenseApprovalStage approvalStage,
            ApprovalStatus crtApprovalStatus
    );

    // =========================================================
    // ACCOUNTS APPROVAL QUEUE
    // =========================================================

    List<ProjectExpense>
    findByApprovalStageAndAccountsApprovalStatusOrderByExpenseDateDesc(
            ExpenseApprovalStage approvalStage,
            ApprovalStatus accountsApprovalStatus
    );

    // =========================================================
    // COMPLETED / OVERALL STATUS
    // =========================================================

    List<ProjectExpense>
    findByApprovalStageAndApprovalStatusOrderByExpenseDateDesc(
            ExpenseApprovalStage approvalStage,
            ApprovalStatus approvalStatus
    );

    // =========================================================
    // PAYMENT QUEUE
    // =========================================================

    List<ProjectExpense> findByPaymentStatusOrderByExpenseDateDesc(
            ExpensePaymentStatus paymentStatus
    );

    List<ProjectExpense> findByPaymentStatusInOrderByExpenseDateDesc(
            List<ExpensePaymentStatus> paymentStatuses
    );
}