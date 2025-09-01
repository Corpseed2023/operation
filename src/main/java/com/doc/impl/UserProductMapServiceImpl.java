package com.doc.impl;

import com.doc.dto.product.UserProductMapRequestDto;
import com.doc.dto.product.UserProductMapResponseDto;
import com.doc.entity.product.Product;
import com.doc.entity.user.User;
import com.doc.entity.user.UserProductMap;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;

import com.doc.repository.ProductRepository;
import com.doc.repository.UserProductMapRepository;
import com.doc.repository.UserRepository;
import com.doc.service.UserProductMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing user-product mappings.
 */
@Service
@Transactional
public class UserProductMapServiceImpl implements UserProductMapService {

    private static final Logger logger = LoggerFactory.getLogger(UserProductMapServiceImpl.class);

    @Autowired
    private UserProductMapRepository userProductMapRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public List<UserProductMapResponseDto> createUserProductMaps(UserProductMapRequestDto requestDto) {
        logger.info("Creating user-product mappings for userIds: {}, productIds: {}", requestDto.getUserIds(), requestDto.getProductIds());

        validateRequestDto(requestDto);

        User createdBy = userRepository.findByIdAndIsDeletedFalse(requestDto.getCreatedBy())
                .orElseThrow(() -> {
                    logger.error("Created by user with ID {} not found or is deleted", requestDto.getCreatedBy());
                    return new ResourceNotFoundException("Created by user with ID " + requestDto.getCreatedBy() + " not found or is deleted");
                });

        User updatedBy = userRepository.findByIdAndIsDeletedFalse(requestDto.getUpdatedBy())
                .orElseThrow(() -> {
                    logger.error("Updated by user with ID {} not found or is deleted", requestDto.getUpdatedBy());
                    return new ResourceNotFoundException("Updated by user with ID " + requestDto.getUpdatedBy() + " not found or is deleted");
                });

        List<UserProductMapResponseDto> responseDtos = new ArrayList<>();

        for (Long userId : requestDto.getUserIds()) {
            User user = userRepository.findByIdAndIsDeletedFalse(userId)
                    .orElseThrow(() -> {
                        logger.error("User with ID {} not found or is deleted", userId);
                        return new ResourceNotFoundException("User with ID " + userId + " not found or is deleted");
                    });

            for (Long productId : requestDto.getProductIds()) {
                if (userProductMapRepository.existsByUserIdAndProductIdAndIsDeletedFalse(userId, productId)) {
                    logger.warn("Mapping for userId: {} and productId: {} already exists, skipping", userId, productId);
                    continue;  // Skip existing mapping
                }

                Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                        .orElseThrow(() -> {
                            logger.error("Product with ID {} not found or is deleted", productId);
                            return new ResourceNotFoundException("Product with ID " + productId + " not found or is deleted");
                        });

                UserProductMap mapping = new UserProductMap();
                mapping.setUser(user);
                mapping.setProduct(product);
                mapping.setRating(requestDto.getRating());
                mapping.setCreatedBy(createdBy.getId());
                mapping.setUpdatedBy(updatedBy.getId());
                mapping.setCreatedDate(new Date());
                mapping.setUpdatedDate(new Date());
                mapping.setDeleted(false);

                mapping = userProductMapRepository.save(mapping);
                logger.info("User-product mapping created successfully with ID: {}", mapping.getId());
                responseDtos.add(mapToResponseDto(mapping));
            }
        }
        return responseDtos;
    }


    @Override
    public UserProductMapResponseDto getUserProductMapById(Long id) {
        logger.info("Fetching user-product mapping with ID: {}", id);
        UserProductMap mapping = userProductMapRepository.findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> {
                    logger.error("User-product mapping with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("User-product mapping with ID " + id + " not found or is deleted");
                });
        return mapToResponseDto(mapping);
    }

    @Override
    public List<UserProductMapResponseDto> getAllUserProductMaps() {
        logger.info("Fetching all user-product mappings");
        List<UserProductMap> mappings = userProductMapRepository.findAll()
                .stream()
                .filter(m -> !m.isDeleted())
                .collect(Collectors.toList());
        return mappings.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Note: This update method expects a single userId and productId,
     * so you might want a different DTO or overload if bulk updates are required.
     */
    @Override
    public UserProductMapResponseDto updateUserProductMap(Long id, UserProductMapRequestDto requestDto) {
        logger.info("Updating user-product mapping with ID: {}", id);
        validateSingleMappingRequestDto(requestDto);

        UserProductMap mapping = userProductMapRepository.findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> {
                    logger.error("User-product mapping with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("User-product mapping with ID " + id + " not found or is deleted");
                });

        Long requestUserId = requestDto.getUserIds().get(0);
        Long requestProductId = requestDto.getProductIds().get(0);

        if (!mapping.getUser().getId().equals(requestUserId) || !mapping.getProduct().getId().equals(requestProductId)) {
            if (userProductMapRepository.existsByUserIdAndProductIdAndIsDeletedFalse(requestUserId, requestProductId)) {
                logger.warn("Mapping for userId: {} and productId: {} already exists", requestUserId, requestProductId);
                throw new ValidationException("Mapping for user ID " + requestUserId + " and product ID " + requestProductId + " already exists");
            }
        }

        User user = userRepository.findByIdAndIsDeletedFalse(requestUserId)
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", requestUserId);
                    return new ResourceNotFoundException("User with ID " + requestUserId + " not found or is deleted");
                });

        Product product = productRepository.findByIdAndIsDeletedFalse(requestProductId)
                .orElseThrow(() -> {
                    logger.error("Product with ID {} not found or is deleted", requestProductId);
                    return new ResourceNotFoundException("Product with ID " + requestProductId + " not found or is deleted");
                });

        User updatedBy = userRepository.findByIdAndIsDeletedFalse(requestDto.getUpdatedBy())
                .orElseThrow(() -> {
                    logger.error("Updated by user with ID {} not found or is deleted", requestDto.getUpdatedBy());
                    return new ResourceNotFoundException("Updated by user with ID " + requestDto.getUpdatedBy() + " not found or is deleted");
                });

        mapping.setUser(user);
        mapping.setProduct(product);
        mapping.setRating(requestDto.getRating());
        mapping.setUpdatedBy(updatedBy.getId());
        mapping.setUpdatedDate(new Date());

        mapping = userProductMapRepository.save(mapping);
        logger.info("User-product mapping updated successfully with ID: {}", id);
        return mapToResponseDto(mapping);
    }

    @Override
    public void deleteUserProductMap(Long id) {
        logger.info("Deleting user-product mapping with ID: {}", id);
        UserProductMap mapping = userProductMapRepository.findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> {
                    logger.error("User-product mapping with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("User-product mapping with ID " + id + " not found or is deleted");
                });

        mapping.setDeleted(true);
        mapping.setUpdatedDate(new Date());
        userProductMapRepository.save(mapping);
        logger.info("User-product mapping deleted successfully with ID: {}", id);
    }

    /**
     * Validation for bulk create request
     */
    private void validateRequestDto(UserProductMapRequestDto requestDto) {
        logger.debug("Validating bulk user-product mapping request DTO: userIds: {}, productIds: {}", requestDto.getUserIds(), requestDto.getProductIds());

        if (requestDto.getUserIds() == null || requestDto.getUserIds().isEmpty()) {
            logger.warn("User IDs list is null or empty");
            throw new ValidationException("User IDs list cannot be null or empty");
        }
        if (requestDto.getProductIds() == null || requestDto.getProductIds().isEmpty()) {
            logger.warn("Product IDs list is null or empty");
            throw new ValidationException("Product IDs list cannot be null or empty");
        }
        if (requestDto.getRating() != null && (requestDto.getRating() < 0 || requestDto.getRating() > 5)) {
            logger.warn("Invalid rating: {}", requestDto.getRating());
            throw new ValidationException("Rating must be between 0 and 5");
        }
        if (requestDto.getCreatedBy() == null) {
            logger.warn("Created by user ID is null");
            throw new ValidationException("Created by user ID cannot be null");
        }
        if (requestDto.getUpdatedBy() == null) {
            logger.warn("Updated by user ID is null");
            throw new ValidationException("Updated by user ID cannot be null");
        }
        logger.debug("User-product mapping request DTO validated successfully");
    }

    /**
     * Validation for single mapping requests like update
     */
    private void validateSingleMappingRequestDto(UserProductMapRequestDto requestDto) {
        logger.debug("Validating single user-product mapping request DTO: userIds: {}, productIds: {}", requestDto.getUserIds(), requestDto.getProductIds());

        if (requestDto.getUserIds() == null || requestDto.getUserIds().size() != 1) {
            logger.warn("User IDs list is null or does not contain exactly one element");
            throw new ValidationException("Exactly one user ID must be provided");
        }
        if (requestDto.getProductIds() == null || requestDto.getProductIds().size() != 1) {
            logger.warn("Product IDs list is null or does not contain exactly one element");
            throw new ValidationException("Exactly one product ID must be provided");
        }
        if (requestDto.getRating() != null && (requestDto.getRating() < 0 || requestDto.getRating() > 5)) {
            logger.warn("Invalid rating: {}", requestDto.getRating());
            throw new ValidationException("Rating must be between 0 and 5");
        }
        if (requestDto.getUpdatedBy() == null) {
            logger.warn("Updated by user ID is null");
            throw new ValidationException("Updated by user ID cannot be null");
        }
        logger.debug("Single user-product mapping request DTO validated successfully");
    }

    private UserProductMapResponseDto mapToResponseDto(UserProductMap mapping) {
        UserProductMapResponseDto dto = new UserProductMapResponseDto();
        dto.setId(mapping.getId());
        dto.setUserId(mapping.getUser().getId());
        dto.setUserName(mapping.getUser().getFullName());
        dto.setProductId(mapping.getProduct().getId());
        dto.setProductName(mapping.getProduct().getProductName());
        dto.setRating(mapping.getRating());
        dto.setCreatedBy(mapping.getCreatedBy());
        dto.setUpdatedBy(mapping.getUpdatedBy());
        dto.setCreatedDate(mapping.getCreatedDate());
        dto.setUpdatedDate(mapping.getUpdatedDate());
        dto.setDeleted(mapping.isDeleted());
        return dto;
    }
}
