package com.doc.service.project;

import com.doc.dto.project.directory.ProjectDirectoryResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectDirectoryService {

    ProjectDirectoryResponseDto createDirectory(
            Long projectId,
            String directoryName,
            Long userId
    );

    ProjectDirectoryResponseDto uploadDocuments(
            Long projectId,
            Long directoryId,
            Long userId,
            List<MultipartFile> files
    );

    List<ProjectDirectoryResponseDto> getProjectDirectories(
            Long projectId
    );
}