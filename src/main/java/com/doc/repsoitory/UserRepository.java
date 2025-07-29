package com.doc.repsoitory;

import com.doc.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailAndIsDeletedFalse(String email);

    Optional<User> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = false")
    Optional<User> findActiveUserById(@Param("id") Long id);

    Page<User> findByIsDeletedFalse(Pageable pageable);

    Page<User> findByDepartmentsIdAndIsDeletedFalse(Long departmentId, Pageable pageable);

    Page<User> findByIsManagerAndIsDeletedFalse(boolean isManager, Pageable pageable);




}