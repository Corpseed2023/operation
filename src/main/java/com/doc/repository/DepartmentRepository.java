package com.doc.repository;

import com.doc.entity.user.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByNameAndIsDeletedFalse(String name);

    Page<Department> findByIsDeletedFalse(Pageable pageable);

    boolean existsByIdAndIsDeletedFalse(Long id);

    Optional<Department> findByIdAndIsDeletedFalse(Long id);
}


