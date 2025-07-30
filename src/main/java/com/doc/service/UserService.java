package com.doc.service;


import com.doc.dto.user.UserRequestDto;
import com.doc.dto.user.UserResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {

    UserResponseDto createUser(UserRequestDto requestDto);

    UserResponseDto getUserById(Long id);

    List<UserResponseDto> getAllUsers(int page, int size, String fullName, String email, Boolean isManager);

    UserResponseDto updateUser(Long id, UserRequestDto requestDto);

    void deleteUser(Long id);
}