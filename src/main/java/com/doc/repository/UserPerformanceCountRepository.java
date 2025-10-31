package com.doc.repository;


import com.doc.entity.project.UserPerformanceCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPerformanceCountRepository extends JpaRepository<UserPerformanceCount, Long> {
    UserPerformanceCount findByUserIdAndProductId(Long userId, Long productId);
}