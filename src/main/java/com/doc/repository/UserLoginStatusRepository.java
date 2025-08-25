package com.doc.repository;



import com.doc.entity.user.UserLoginStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLoginStatusRepository extends JpaRepository<UserLoginStatus, Long> {

    /**
     * Find user online status by user ID.
     *
     * @param userId the user ID
     * @return Optional containing the UserOnlineStatus if found
     */
    Optional<UserLoginStatus> findByUserIdAndIsDeletedFalse(Long userId);
}