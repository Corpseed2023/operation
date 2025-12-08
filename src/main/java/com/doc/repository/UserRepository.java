package com.doc.repository;

import com.doc.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailAndIsActiveTrueAndIsDeletedFalse(String email);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isActive = true AND u.isDeleted = false")
    Optional<User> findActiveUserById(@Param("id") Long id);

    Page<User> findByIsActiveTrueAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.departments d WHERE d.id = :departmentId AND u.isActive = true AND u.isDeleted = false")
    List<User> findByDepartmentsIdAndIsActiveTrueAndIsDeletedFalse(@Param("departmentId") Long departmentId);

    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId AND u.isActive = true AND u.isDeleted = false")
    Page<User> findByManagerIdAndIsDeletedFalseList(@Param("managerId") Long managerId, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.departments d WHERE d.id IN :deptIds AND u.isActive = true AND u.isDeleted = false")
    List<User> findByDepartmentIdsIn(@Param("deptIds") List<Long> deptIds);

    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId AND u.isActive = true AND u.isDeleted = false")
    List<User> findByManagerIdAndIsDeletedFalse(@Param("managerId") Long managerId);
}