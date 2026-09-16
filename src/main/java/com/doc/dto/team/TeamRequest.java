package com.doc.dto.team;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DTO for creating or updating a team.
 */
@Getter
@Setter
@NoArgsConstructor
public class TeamRequest {

    private String name;
    private Long departmentId;
    private Long teamLeadId;
    private List<Long> memberIds = new ArrayList<>();
    private List<Long> productIds = new ArrayList<>();
    private boolean isActive = true;
    private boolean isTemporary = false; // Indicates if team is temporary
    private Date endDate; // End date for temporary teams (optional)
    private Long parentTeamId;        // set when creating/updating a sub-team
    private Integer sequence;         // position within parent group
    private boolean isGroup;          // true when this team is being created as a routing group
    private List<Long> subTeamIds;    // optional: convenience for assigning sub-teams at group-creation time, in order
    private Long createdBy;
    private Long updatedBy;
    private Date createdDate;
    private Date updatedDate;
}