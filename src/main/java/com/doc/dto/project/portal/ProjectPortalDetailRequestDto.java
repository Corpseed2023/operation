package com.doc.dto.project.portal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectPortalDetailRequestDto {

    @NotBlank(message = "Portal name is required")
    @Size(max = 255)
    private String portalName;

    @Size(max = 512)
    private String portalUrl;

    @NotBlank(message = "Username is required")
    @Size(max = 255)
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @Size(max = 1000)
    private String remarks;
}