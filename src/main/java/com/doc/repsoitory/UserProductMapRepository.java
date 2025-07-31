package com.doc.repsoitory;

import com.doc.entity.user.UserProductMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing {@link UserProductMap} entities.
 */
@Repository
public interface UserProductMapRepository extends JpaRepository<UserProductMap, Long> {

    /**
     * Finds non-deleted mappings by product ID, ordered by rating descending.
     *
     * @param productId the product ID
     * @return list of non-deleted user-product mappings
     */
    @Query("SELECT upm FROM UserProductMap upm WHERE upm.product.id = :productId AND upm.isDeleted = false ORDER BY upm.rating DESC")
    List<UserProductMap> findByProductIdAndIsDeletedFalse(@Param("productId") Long productId);

    /**
     * Checks if a mapping exists for the given user and product.
     *
     * @param userId    the user ID
     * @param productId the product ID
     * @return true if a non-deleted mapping exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(upm) > 0 THEN true ELSE false END FROM UserProductMap upm WHERE upm.user.id = :userId AND upm.product.id = :productId AND upm.isDeleted = false")
    boolean existsByUserIdAndProductIdAndIsDeletedFalse(@Param("userId") Long userId, @Param("productId") Long productId);


}
