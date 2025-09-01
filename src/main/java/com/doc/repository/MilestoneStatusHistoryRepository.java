package com.doc.repository;

import com.doc.entity.project.MilestoneStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MilestoneStatusHistoryRepository extends JpaRepository<MilestoneStatusHistory, Long> {
}