package com.doc.service;


import com.doc.dto.user.UserLoginStatusResponseDto;

public interface UserLoginStatusService {

    /**
     * Set the user status to online.
     *
     * @param userId the ID of the user
     * @return UserOnlineStatusResponseDto with updated status
     */
    UserLoginStatusResponseDto setOnline(Long userId);

    /**
     * Set the user status to offline.
     *
     * @param userId the ID of the user
     * @return UserOnlineStatusResponseDto with updated status
     */
    UserLoginStatusResponseDto setOffline(Long userId);

    /**
     * Get the current online status of the user.
     *
     * @param userId the ID of the user
     * @return UserOnlineStatusResponseDto with current status
     */
    UserLoginStatusResponseDto getStatus(Long userId);
}