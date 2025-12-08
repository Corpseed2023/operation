package com.doc.dto.project.portal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectPortalDetailResponseDto {

    private Long id;
    private Long projectId;
    private String portalName;
    private String portalUrl;
    private String username;
    private String password; // Always masked
    private String remarks;
    private Date createdDate;
    private String createdByName;
    private Date updatedDate;
    private String updatedByName;

    private String status;
    private String approvedByName;
    private Date approvalDate;
    private String approvalRemarks;
}