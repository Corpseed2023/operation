package com.doc.dto.user;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginStatusResponseDto {

    private Long userId;

    private boolean isOnline;

    private Date lastOnline;
}
