package com.doc.dto.project;


import lombok.Data;

@Data
public class ProjectCountResponseDto {
    private String totalProject;
    private String openProject;
    private String inProgressProject;
    private String completedProject;
}
