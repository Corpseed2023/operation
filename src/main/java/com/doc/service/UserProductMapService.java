package com.doc.service;

import com.doc.dto.product.UserProductMapRequestDto;
import com.doc.dto.product.UserProductMapResponseDto;

import java.util.List;

/**
 * Service interface for managing user-product mappings.
 */
public interface UserProductMapService {

    /**
     * Creates new user-product mappings in bulk.
     *
     * @param requestDto the request DTO containing lists of userIds and productIds, plus other details
     * @return the list of created mapping details
     */
    List<UserProductMapResponseDto> createUserProductMaps(UserProductMapRequestDto requestDto);

    /**
     * Retrieves a user-product mapping by its ID.
     *
     * @param id the mapping ID
     * @return the mapping details
     */
    UserProductMapResponseDto getUserProductMapById(Long id);

    /**
     * Retrieves all user-product mappings.
     *
     * @return a list of all non-deleted mappings
     */
    List<UserProductMapResponseDto> getAllUserProductMaps();

    /**
     * Updates an existing user-product mapping.
     *
     * @param id         the mapping ID
     * @param requestDto the request DTO containing updated details (expects single userId and productId)
     * @return the updated mapping details
     */
    UserProductMapResponseDto updateUserProductMap(Long id, UserProductMapRequestDto requestDto);

    /**
     * Deletes a user-product mapping (soft delete).
     *
     * @param id the mapping ID
     */
    void deleteUserProductMap(Long id);
}
