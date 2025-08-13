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

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Role> findByIdAndName(Long id, String name);

    Page<Role> findAll(Pageable pageable);

    @Query("SELECT r FROM Role r WHERE (:name IS NULL OR r.name LIKE %:name%)")
    Page<Role> findByNameContaining(@Param("name") String name, Pageable pageable);
}
