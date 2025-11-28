package com.doc.dto.project.portal;

import lombok.Data;

import java.util.Date;

@Data
public class ProjectPortalDetailResponseDto {

    private Long id;
    private Long projectId;
    private String portalName;
    private String portalUrl;
    private String username;
    private String password;           // Will be masked as "••••••••"
    private String remarks;
    private Date createdDate;
    private String createdByName;
    private Date updatedDate;
    private String updatedByName;
}