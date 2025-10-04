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

    /**
     * Checks if a user with the given email exists, is active, and is not deleted.
     *
     * @param email the email address to check
     * @return true if a user with the specified email exists, is active, and is not deleted, false otherwise
     */
    boolean existsByEmailAndIsActiveTrueAndIsDeletedFalse(String email);


    /**
     * Finds an active user by ID if active and not deleted (for compatibility with services).
     *
     * @param id the user ID
     * @return an Optional containing the user if found, active, and not deleted, empty otherwise
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isActive = true AND u.isDeleted = false")
    Optional<User> findActiveUserById(@Param("id") Long id);

    /**
     * Finds all active and non-deleted users with pagination.
     *
     * @param pageable pagination information
     * @return a page of active and non-deleted users
     */
    Page<User> findByIsActiveTrueAndIsDeletedFalse(Pageable pageable);


    /**
     * Finds active and non-deleted users by department ID (non-paginated).
     *
     * @param departmentId the department ID
     * @return a list of active and non-deleted users in the specified department
     */
    @Query("SELECT u FROM User u JOIN u.departments d WHERE d.id = :departmentId AND u.isActive = true AND u.isDeleted = false")
    List<User> findByDepartmentsIdAndIsActiveTrueAndIsDeletedFalse(@Param("departmentId") Long departmentId);


    /**
     * Finds users with the ADMIN role who are active and not deleted.
     *
     * @return a list of active and non-deleted users with the ADMIN role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ADMIN' AND u.isActive = true AND u.isDeleted = false")
    List<User> findAdmins();

    /**
     * Finds active and non-deleted users managed by a specific manager with pagination.
     *
     * @param managerId the ID of the manager
     * @param pageable pagination information
     * @return a page of active and non-deleted users managed by the specified manager
     */
    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId AND u.isActive = true AND u.isDeleted = false")
    Page<User> findByManagerIdAndIsDeletedFalseList(@Param("managerId") Long managerId, Pageable pageable);

    /**
     * Finds active and non-deleted users by department IDs.
     *
     * @param deptIds the list of department IDs
     * @return a list of active and non-deleted users in the specified departments
     */
    @Query("SELECT u FROM User u JOIN u.departments d WHERE d.id IN :deptIds AND u.isActive = true AND u.isDeleted = false")
    List<User> findByDepartmentIdsIn(@Param("deptIds") List<Long> deptIds);

    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId AND u.isActive = true AND u.isDeleted = false")
    List<User> findByManagerIdAndIsDeletedFalse(@Param("managerId") Long managerId);
}