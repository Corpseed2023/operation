package com.doc.repository.department;

import com.doc.entity.department.DepartmentAutoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentAutoConfigRepository extends JpaRepository<DepartmentAutoConfig, Long> {
    Optional<DepartmentAutoConfig> findByDepartmentIdAndIsDeletedFalse(Long departmentId);
}