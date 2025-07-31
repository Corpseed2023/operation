package com.doc.impl;

import com.doc.dto.product.UserProductMapRequestDto;
import com.doc.dto.product.UserProductMapResponseDto;
import com.doc.entity.product.Product;
import com.doc.entity.user.User;
import com.doc.entity.user.UserProductMap;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;

import com.doc.repsoitory.ProductRepository;
import com.doc.repsoitory.UserProductMapRepository;
import com.doc.repsoitory.UserRepository;
import com.doc.service.UserProductMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public UserProductMapResponseDto createUserProductMap(UserProductMapRequestDto requestDto) {
        logger.info("Creating user-product mapping for userId: {}, productId: {}", requestDto.getUserId(), requestDto.getProductId());
        validateRequestDto(requestDto);

        if (userProductMapRepository.existsByUserIdAndProductIdAndIsDeletedFalse(requestDto.getUserId(), requestDto.getProductId())) {
            logger.warn("Mapping for userId: {} and productId: {} already exists", requestDto.getUserId(), requestDto.getProductId());
            throw new ValidationException("Mapping for user ID " + requestDto.getUserId() + " and product ID " + requestDto.getProductId() + " already exists");
        }

        User user = userRepository.findByIdAndIsDeletedFalse(requestDto.getUserId())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", requestDto.getUserId());
                    return new ResourceNotFoundException("User with ID " + requestDto.getUserId() + " not found or is deleted");
                });

        Product product = productRepository.findByIdAndIsDeletedFalse(requestDto.getProductId())
                .orElseThrow(() -> {
                    logger.error("Product with ID {} not found or is deleted", requestDto.getProductId());
                    return new ResourceNotFoundException("Product with ID " + requestDto.getProductId() + " not found or is deleted");
                });

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
        return mapToResponseDto(mapping);
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

    @Override
    public UserProductMapResponseDto updateUserProductMap(Long id, UserProductMapRequestDto requestDto) {
        logger.info("Updating user-product mapping with ID: {}", id);
        validateRequestDto(requestDto);

        UserProductMap mapping = userProductMapRepository.findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> {
                    logger.error("User-product mapping with ID {} not found or is deleted", id);
                    return new ResourceNotFoundException("User-product mapping with ID " + id + " not found or is deleted");
                });

        if (!mapping.getUser().getId().equals(requestDto.getUserId()) || !mapping.getProduct().getId().equals(requestDto.getProductId())) {
            if (userProductMapRepository.existsByUserIdAndProductIdAndIsDeletedFalse(requestDto.getUserId(), requestDto.getProductId())) {
                logger.warn("Mapping for userId: {} and productId: {} already exists", requestDto.getUserId(), requestDto.getProductId());
                throw new ValidationException("Mapping for user ID " + requestDto.getUserId() + " and product ID " + requestDto.getProductId() + " already exists");
            }
        }

        User user = userRepository.findByIdAndIsDeletedFalse(requestDto.getUserId())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", requestDto.getUserId());
                    return new ResourceNotFoundException("User with ID " + requestDto.getUserId() + " not found or is deleted");
                });

        Product product = productRepository.findByIdAndIsDeletedFalse(requestDto.getProductId())
                .orElseThrow(() -> {
                    logger.error("Product with ID {} not found or is deleted", requestDto.getProductId());
                    return new ResourceNotFoundException("Product with ID " + requestDto.getProductId() + " not found or is deleted");
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

    private void validateRequestDto(UserProductMapRequestDto requestDto) {
        logger.debug("Validating user-product mapping request DTO: userId: {}, productId: {}", requestDto.getUserId(), requestDto.getProductId());
        if (requestDto.getUserId() == null) {
            logger.warn("User ID is null");
            throw new ValidationException("User ID cannot be null");
        }
        if (requestDto.getProductId() == null) {
            logger.warn("Product ID is null");
            throw new ValidationException("Product ID cannot be null");
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
