package com.doc.dto.project.directory;

import com.doc.dto.document.DocumentUploadResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProjectDirectoryResponseDto {

    private Long directoryId;
    private Long projectId;
    private String directoryName;
    private Long createdBy;

    private List<DocumentUploadResponse> documents = new ArrayList<>();
}