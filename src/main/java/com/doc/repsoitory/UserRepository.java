package com.doc.repsoitory;

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
     * Checks if a user with the given email exists and is not deleted.
     *
     * @param email the email address to check
     * @return true if a user with the specified email exists and is not deleted, false otherwise
     */
    boolean existsByEmailAndIsDeletedFalse(String email);

    /**
     * Finds a user by ID if not deleted.
     *
     * @param id the user ID
     * @return an Optional containing the user if found and not deleted, empty otherwise
     */
    Optional<User> findByIdAndIsDeletedFalse(Long id);

    /**
     * Finds an active user by ID if not deleted (for compatibility with services).
     *
     * @param id the user ID
     * @return an Optional containing the user if found and not deleted, empty otherwise
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = false")
    Optional<User> findActiveUserById(@Param("id") Long id);

    /**
     * Finds all non-deleted users with pagination.
     *
     * @param pageable pagination information
     * @return a page of non-deleted users
     */
    Page<User> findByIsDeletedFalse(Pageable pageable);

    /**
     * Finds non-deleted users by department ID with pagination.
     *
     * @param departmentId the department ID
     * @param pageable     pagination information
     * @return a page of non-deleted users in the specified department
     */
    Page<User> findByDepartmentsIdAndIsDeletedFalse(Long departmentId, Pageable pageable);

    /**
     * Finds non-deleted users by manager flag with pagination.
     *
     * @param managerFlag the manager flag
     * @param pageable    pagination information
     * @return a page of non-deleted users with the specified manager flag
     */
    Page<User> findByManagerFlagAndIsDeletedFalse(boolean managerFlag, Pageable pageable);

    /**
     * Finds users by filters (full name, email, manager flag) with pagination.
     *
     * @param fullName    the full name to filter by (optional)
     * @param email       the email to filter by (optional)
     * @param managerFlag the manager flag to filter by (optional)
     * @param pageable    pagination information
     * @return a page of non-deleted users matching the filters
     */
    @Query("SELECT u FROM User u WHERE " +
            "(:fullName IS NULL OR u.fullName LIKE %:fullName%) AND " +
            "(:email IS NULL OR u.email = :email) AND " +
            "(:managerFlag IS NULL OR u.managerFlag = :managerFlag) AND " +
            "u.isDeleted = false")
    Page<User> findByFilters(
            @Param("fullName") String fullName,
            @Param("email") String email,
            @Param("managerFlag") Boolean managerFlag,
            Pageable pageable);

    /**
     * Finds users with the ADMIN role who are not deleted.
     *
     * @return a list of non-deleted users with the ADMIN role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ADMIN' AND u.isDeleted = false")
    List<User> findAdmins();
}
