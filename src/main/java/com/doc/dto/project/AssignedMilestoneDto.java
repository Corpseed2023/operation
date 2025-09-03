package com.doc.dto.project;


import com.doc.dto.user.UserResponseDto;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignedMilestoneDto {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long milestoneId;
    private String milestoneName;
    private String status;
    private String statusReason;
    private String visibilityReason;
    private Integer reworkAttempts;
    private Date visibleDate;
    private Date startedDate;
    private Date completedDate;
    private List<DocumentResponseDto> documents;
    private UserResponseDto assignedUser;
}
