package com.doc.repsoitory;


import com.doc.entity.user.Designation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {

    // Check if a designation with the given name and department exists (non-deleted)
    boolean existsByNameAndDepartmentIdAndIsDeletedFalse(String name, Long departmentId);

    // Fetch non-deleted designations with pagination, ensuring department is not deleted
    @Query("SELECT d FROM Designation d JOIN d.department dept WHERE d.isDeleted = false AND dept.isDeleted = false")
    Page<Designation> findByDepartmentIsDeletedFalse(Pageable pageable);

    // Fetch non-deleted designations by department ID with pagination
    @Query("SELECT d FROM Designation d WHERE d.department.id = :departmentId AND d.isDeleted = false")
    Page<Designation> findByDepartmentIdAndIsDeletedFalse(Long departmentId, Pageable pageable);

    // Fetch all non-deleted designations by department ID without pagination
    @Query("SELECT d FROM Designation d WHERE d.department.id = :departmentId AND d.isDeleted = false")
    List<Designation> findByDepartmentIdAndIsDeletedFalse(Long departmentId);

    // Fetch designations by name (partial match) and non-deleted
    @Query("SELECT d FROM Designation d WHERE d.name LIKE %:name% AND d.isDeleted = false")
    List<Designation> findByNameContainingAndIsDeletedFalse(String name);
}
