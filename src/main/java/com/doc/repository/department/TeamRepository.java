package com.doc.repository.department;

import com.doc.entity.department.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByDepartmentIdAndIsDeletedFalse(Long departmentId);
    Optional<Team> findByIdAndIsDeletedFalse(Long id);

    boolean existsByNameAndDepartmentIdAndIsDeletedFalse(String name, Long departmentId);
}