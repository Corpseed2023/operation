package com.doc.repository.projectRepo.activity;

import com.doc.entity.project.activity.ProjectExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectExpenseRepository extends JpaRepository<ProjectExpense, Long> {

    Optional<ProjectExpense> findByActivityId(Long activityId);
}
