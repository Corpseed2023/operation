package com.doc.repository;

import com.doc.entity.user.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByNameAndIsDeletedFalse(String name);

    Page<Department> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT d FROM Department d WHERE d.id = :id AND d.isDeleted = false")
    Optional<Department> findByIdAndIsDeletedFalse(@Param("id") Long id);
}