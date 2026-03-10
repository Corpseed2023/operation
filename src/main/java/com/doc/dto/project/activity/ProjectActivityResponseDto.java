package com.doc.dto.project.activity;

import com.doc.em.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectActivityResponseDto {

    private Long activityId;

    private ActivityType activityType;

    private String title;

    private String summary;

    private LocalDateTime activityDate;

    private Long createdByUserId;

    private String createdByUserName;

    private Object details;
}