package com.doc.repository;

import com.doc.entity.project.ProjectAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectAssignmentHistoryRepository extends JpaRepository<ProjectAssignmentHistory, Long> {

    @Query("SELECT p FROM ProjectAssignmentHistory p WHERE p.assignedUser.id = :userId AND p.project.id = :projectId")
    ProjectAssignmentHistory findByUserIdAndProjectId(@Param("userId") Long userId, @Param("projectId") Long projectId);
}
