package com.doc.repository.projectRepo;

import com.doc.entity.project.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectStatusRepository extends JpaRepository<ProjectStatus, Long> {

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByNameIgnoreCase(String name);

    Optional<ProjectStatus> findByName(String name);

    @Query("SELECT ps FROM ProjectStatus ps WHERE ps.id = :id")
    Optional<ProjectStatus> findActiveById(Long id);

    Page<ProjectStatus> findAll(Pageable pageable);
}