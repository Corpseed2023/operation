package com.doc.repository;

import com.doc.entity.project.ProjectDocumentUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDocumentUploadRepository extends JpaRepository<ProjectDocumentUpload, Long> {
    Optional<ProjectDocumentUpload> findByIdAndIsDeletedFalse(Long id);
    List<ProjectDocumentUpload> findByMilestoneAssignmentIdAndIsDeletedFalse(Long milestoneAssignmentId);
}