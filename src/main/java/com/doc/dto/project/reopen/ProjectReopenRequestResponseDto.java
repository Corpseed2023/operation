package com.doc.dto.project.reopen;

import com.doc.em.ProjectReopenRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ProjectReopenRequestResponseDto {

    private Long id;

    private Long projectId;
    private String projectName;
    private String projectNo;

    private Long detectedAtAssignmentId;
    private String detectedAtMilestoneName;

    private Long responsibleAssignmentId;
    private String responsibleMilestoneName;

    private Long requestedById;
    private String requestedByName;

    private Long requesterManagerId;
    private String requesterManagerName;

    private Long responsibleManagerId;
    private String responsibleManagerName;

    private String requestReason;
    private String requesterManagerRemarks;
    private String responsibleManagerRemarks;

    private ProjectReopenRequestStatus status;

    private Date requestedAt;
    private Date requesterManagerActionAt;
    private Date responsibleManagerActionAt;

    private Date createdDate;
    private Date updatedDate;
}