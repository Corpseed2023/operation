package com.doc.dto.team;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class AssignmentResponse {
    private Long assignedUserId;
    private String assignedUserName;
    private Long resolvedFromTeamId;
}
