package com.doc.impl;

import com.doc.dto.user.UserLoginStatusResponseDto;
import com.doc.entity.user.UserLoginStatus;

import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.repository.UserLoginStatusRepository;
import com.doc.repository.UserRepository;
import com.doc.service.UserLoginStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class UserOnlineStatusServiceImpl implements UserLoginStatusService {

    private static final Logger logger = LoggerFactory.getLogger(UserOnlineStatusServiceImpl.class);

    @Autowired
    private UserLoginStatusRepository userOnlineStatusRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserLoginStatusResponseDto setOnline(Long userId) {
        logger.info("Setting user ID: {} to online", userId);
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found or is deleted", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found or is deleted");
                });

        UserLoginStatus status = userOnlineStatusRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseGet(() -> createNewStatus(user));

        status.setOnline(true);
        status.setLastOnline(new Date());
        status.setUpdatedDate(new Date());

        UserLoginStatus savedStatus = userOnlineStatusRepository.save(status);
        logger.info("User ID: {} set to online successfully", userId);
        return mapToDto(savedStatus);
    }

    @Override
    public UserLoginStatusResponseDto setOffline(Long userId) {
        logger.info("Setting user ID: {} to offline", userId);
        UserLoginStatus status = userOnlineStatusRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    logger.error("Online status for user ID {} not found", userId);
                    return new ResourceNotFoundException("Online status for user ID " + userId + " not found");
                });

        status.setOnline(false);
        status.setLastOnline(new Date());
        status.setUpdatedDate(new Date());

        UserLoginStatus savedStatus = userOnlineStatusRepository.save(status);
        logger.info("User ID: {} set to offline successfully", userId);
        return mapToDto(savedStatus);
    }

    @Override
    public UserLoginStatusResponseDto getStatus(Long userId) {
        logger.info("Fetching online status for user ID: {}", userId);
        UserLoginStatus status = userOnlineStatusRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    logger.error("Online status for user ID {} not found", userId);
                    return new ResourceNotFoundException("Online status for user ID " + userId + " not found");
                });
        return mapToDto(status);
    }

    private UserLoginStatus createNewStatus(User user) {
        logger.debug("Creating new online status for user ID: {}", user.getId());
        UserLoginStatus newStatus = new UserLoginStatus();
        newStatus.setUser(user);
        newStatus.setOnline(false);
        newStatus.setCreatedDate(new Date());
        newStatus.setUpdatedDate(new Date());
        newStatus.setDeleted(false);
        return newStatus;
    }

    private UserLoginStatusResponseDto mapToDto(UserLoginStatus status) {
        UserLoginStatusResponseDto dto = new UserLoginStatusResponseDto();
        dto.setUserId(status.getUser().getId());
        dto.setOnline(status.isOnline());
        dto.setLastOnline(status.getLastOnline());
        return dto;
    }
}