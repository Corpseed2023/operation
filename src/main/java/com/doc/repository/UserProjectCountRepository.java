package com.doc.repository;

import com.doc.entity.project.UserProjectCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProjectCountRepository extends JpaRepository<UserProjectCount, Long> {
    UserProjectCount findByUserIdAndProductId(Long userId, Long productId);
}