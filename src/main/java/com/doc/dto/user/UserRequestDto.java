package com.doc.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserRequestDto {

    @NotBlank(message = "Full name cannot be empty")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email must be valid")
    private String email;

    @Size(max = 20, message = "Contact number cannot exceed 20 characters")
    private String contactNo;

    @NotNull(message = "Designation ID cannot be null")
    private Long designationId;

    @NotNull(message = "Department IDs cannot be null")
    private List<Long> departmentIds;

    @NotNull(message = "Role IDs cannot be null")
    private List<Long> roleIds;

    private Long managerId;

    private Boolean managerFlag;
}