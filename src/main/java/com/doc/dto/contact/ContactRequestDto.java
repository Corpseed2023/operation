package com.doc.dto.contact;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactRequestDto {

    private String title;

    @NotBlank(message = "Contact name cannot be empty")
    private String name;

    @NotBlank(message = "Contact email cannot be empty")
    private String emails;

    private String contactNo;

    private String whatsappNo;

    private Long companyId;

    private String designation;

    @NotNull(message = "Created by user ID cannot be null")
    private Long createdBy;

    @NotNull(message = "Updated by user ID cannot be null")
    private Long updatedBy;
}
