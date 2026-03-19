package com.doc.dto.project.activity;

import lombok.Data;

@Data
public class CreateNoteRequestDto {

    private String noteText;

    private Long createdByUserId;

}