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

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN u.departments d
            WHERE d.id = :departmentId
              AND u.managerFlag = true
              AND u.isDeleted = false
              AND u.isActive = true
            """)
    List<User> findActiveManagersByDepartmentId(@Param("departmentId") Long departmentId);

    @Query(value = """
        SELECT COUNT(*)
        FROM erp_operation.users u
        INNER JOIN erp_operation.user_department_map udm
                ON udm.user_id = u.id
        WHERE u.id = :userId
          AND udm.dept_id = :departmentId
          AND u.is_active = 1
          AND u.is_deleted = 0
        """,
            nativeQuery = true)
    Long countActiveUserInDepartment(
            @Param("userId") Long userId,
            @Param("departmentId") Long departmentId
    );
}