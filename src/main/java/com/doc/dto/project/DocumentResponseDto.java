package com.doc.dto.project;

import com.doc.entity.project.ProjectDocumentUpload.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDto {
    private Long id;
    private String fileUrl;
    private DocumentStatus status;
    private String remarks;
    private Date uploadTime;
}