package com.doc.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long id;

    private String fullName;

    private String email;

    private String contactNo;

    private String designation;

    private Long designationId;

    private List<Long> departmentIds;

    private List<Long> roleIds;

    private Long managerId;

    private boolean managerFlag; // Changed from isManager

    private Date createdDate;

    private Date updatedDate;
}