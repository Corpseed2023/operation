package com.doc.repository;


import com.doc.entity.project.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MilestoneStatusRepository extends JpaRepository<MilestoneStatus, Long> {
    Optional<MilestoneStatus> findByName(String name);
}