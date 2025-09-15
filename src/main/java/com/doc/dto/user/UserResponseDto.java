package com.doc.dto.user;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
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
    private boolean managerFlag;
    private Date createdDate;
    private Date updatedDate;
}