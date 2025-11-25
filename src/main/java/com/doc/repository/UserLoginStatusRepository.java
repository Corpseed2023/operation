package com.doc.repository;



import com.doc.entity.user.UserLoginStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLoginStatusRepository extends JpaRepository<UserLoginStatus, Long> {

    /**
     * Find user online status by user ID.
     *
     * @param userId the user ID
     * @return Optional containing the UserOnlineStatus if found
     */
    Optional<UserLoginStatus> findByUserIdAndIsDeletedFalse(Long userId) ;

    List<UserLoginStatus> findAllByIsDeletedFalse();
    @Query("SELECT s FROM UserLoginStatus s WHERE s.user.id = :userId AND s.isDeleted = false " +
            "AND DATE(s.lastOnline) = CURRENT_DATE")
    Optional<UserLoginStatus> findTodayStatusByUserId(@Param("userId") Long userId);


}