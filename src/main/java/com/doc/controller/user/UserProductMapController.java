package com.doc.controller.user;

import com.doc.dto.product.UserProductMapRequestDto;
import com.doc.dto.product.UserProductMapResponseDto;
import com.doc.service.UserProductMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing user-product mappings.
 */
@RestController
@RequestMapping("/api/user-product-mappings")
public class UserProductMapController {

    private static final Logger logger = LoggerFactory.getLogger(UserProductMapController.class);

    @Autowired
    private UserProductMapService userProductMapService;

    /**
     * Creates a new user-product mapping.
     *
     * @param requestDto the request DTO containing mapping details
     * @return the created mapping details
     */
    @PostMapping
    public ResponseEntity<UserProductMapResponseDto> createUserProductMap(@RequestBody UserProductMapRequestDto requestDto) {
        logger.info("Received request to create user-product mapping for userId: {}, productId: {}", requestDto.getUserId(), requestDto.getProductId());
        UserProductMapResponseDto responseDto = userProductMapService.createUserProductMap(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Retrieves a user-product mapping by its ID.
     *
     * @param id the mapping ID
     * @return the mapping details
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProductMapResponseDto> getUserProductMapById(@PathVariable Long id) {
        logger.info("Received request to fetch user-product mapping with ID: {}", id);
        UserProductMapResponseDto responseDto = userProductMapService.getUserProductMapById(id);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Retrieves all user-product mappings.
     *
     * @return a list of all non-deleted mappings
     */
    @GetMapping
    public ResponseEntity<List<UserProductMapResponseDto>> getAllUserProductMaps() {
        logger.info("Received request to fetch all user-product mappings");
        List<UserProductMapResponseDto> responseDtos = userProductMapService.getAllUserProductMaps();
        return ResponseEntity.ok(responseDtos);
    }

    /**
     * Updates an existing user-product mapping.
     *
     * @param id         the mapping ID
     * @param requestDto the request DTO containing updated details
     * @return the updated mapping details
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserProductMapResponseDto> updateUserProductMap(@PathVariable Long id, @RequestBody UserProductMapRequestDto requestDto) {
        logger.info("Received request to update user-product mapping with ID: {}", id);
        UserProductMapResponseDto responseDto = userProductMapService.updateUserProductMap(id, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Deletes a user-product mapping (soft delete).
     *
     * @param id the mapping ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserProductMap(@PathVariable Long id) {
        logger.info("Received request to delete user-product mapping with ID: {}", id);
        userProductMapService.deleteUserProductMap(id);
        return ResponseEntity.noContent().build();
    }
}
