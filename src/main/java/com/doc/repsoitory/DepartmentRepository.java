package com.doc.repsoitory;

import com.doc.entity.user.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByNameAndIsDeletedFalse(String name);
    Page<Department> findByIsDeletedFalse(Pageable pageable);
}