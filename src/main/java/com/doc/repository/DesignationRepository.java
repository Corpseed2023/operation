package com.doc.repository;

import com.doc.entity.department.Designation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {

    // Check if a designation with the given ID exists and is not deleted
    boolean existsByIdAndIsDeletedFalse(Long id);

    // Check if a designation with the given name and department exists (non-deleted)
    boolean existsByNameAndDepartmentIdAndIsDeletedFalse(String name, Long departmentId);


    @Query("SELECT d FROM Designation d WHERE d.id = :id AND d.isDeleted = false")
    Optional<Designation> findActiveUserById(Long id);

    // Fetch all non-deleted designations with pagination
    @Query("SELECT d FROM Designation d WHERE d.isDeleted = false")
    Page<Designation> findByIsDeletedFalse(Pageable pageable);

}