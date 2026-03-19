package com.doc.dto.project.activity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectCommentResponseDto {

    private Long id;
    private String commentText;
    private Long parentCommentId;
    private LocalDateTime createdDate;
    private Long createdByUserId;
    private String createdByUserName;

    private List<ProjectCommentResponseDto> children = new ArrayList<>();
}