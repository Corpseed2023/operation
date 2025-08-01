package com.doc.repsoitory;

import com.doc.entity.project.ProjectMilestoneAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing ProjectMilestoneAssignment entities.
 */
@Repository
public interface ProjectMilestoneAssignmentRepository extends JpaRepository<ProjectMilestoneAssignment, Long> {
}