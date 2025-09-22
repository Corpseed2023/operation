package com.doc.repository;

import com.doc.entity.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByIdAndIsDeletedFalse(Long id);

    boolean existsByNameAndIsDeletedFalseAndIdNot(String name, Long id);

    Optional<Role> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT r FROM Role r WHERE r.isDeleted = false AND LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Role> findByNameContainingIgnoreCaseAndIsDeletedFalse(@Param("name") String name, Pageable pageable);

    Page<Role> findByIsDeletedFalse(Pageable pageable);
}