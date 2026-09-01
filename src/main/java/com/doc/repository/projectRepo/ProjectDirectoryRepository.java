package com.doc.repository.projectRepo;

import com.doc.entity.project.ProjectDirectory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectDirectoryRepository
        extends JpaRepository<ProjectDirectory, Long> {

    Optional<ProjectDirectory> findByIdAndIsDeletedFalse(Long id);

    List<ProjectDirectory>
    findByProjectIdAndIsDeletedFalseOrderByCreatedDateDesc(Long projectId);

    boolean existsByProjectIdAndDirectoryNameIgnoreCaseAndIsDeletedFalse(
            Long projectId,
            String directoryName
    );
}