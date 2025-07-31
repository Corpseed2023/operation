package com.doc.service;

import com.doc.dto.product.UserProductMapRequestDto;
import com.doc.dto.product.UserProductMapResponseDto;

import java.util.List;

/**
 * Service interface for managing user-product mappings.
 */
public interface UserProductMapService {

    UserProductMapResponseDto createUserProductMap(UserProductMapRequestDto requestDto);

    UserProductMapResponseDto getUserProductMapById(Long id);

    List<UserProductMapResponseDto> getUserProductMapsByProductId(Long productId, int page, int size);

    void deleteUserProductMap(Long id);
}
