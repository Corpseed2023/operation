package com.doc.dto.project.activity;

import lombok.Data;

@Data
public class CreateCommentRequestDto {

    private String commentText;

    private Long parentCommentId;

    private Long createdByUserId;

    private String createdByUserName;
}
