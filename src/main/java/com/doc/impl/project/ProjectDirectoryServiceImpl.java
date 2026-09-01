package com.doc.impl.project;

import com.doc.config.S3Service;
import com.doc.dto.document.DocumentUploadResponse;
import com.doc.dto.project.directory.ProjectDirectoryResponseDto;
import com.doc.entity.document.Document;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectDirectory;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.DocumentRepository;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.projectRepo.ProjectDirectoryRepository;
import com.doc.service.project.ProjectDirectoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProjectDirectoryServiceImpl
        implements ProjectDirectoryService {

    private final ProjectDirectoryRepository directoryRepository;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public ProjectDirectoryServiceImpl(
            ProjectDirectoryRepository directoryRepository,
            ProjectRepository projectRepository,
            DocumentRepository documentRepository,
            UserRepository userRepository,
            S3Service s3Service
    ) {
        this.directoryRepository = directoryRepository;
        this.projectRepository = projectRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    @Override
    public ProjectDirectoryResponseDto createDirectory(
            Long projectId,
            String directoryName,
            Long userId
    ) {
        Project project = projectRepository.findById(projectId)
                .filter(existingProject -> !existingProject.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found",
                        "ERR_PROJECT_NOT_FOUND"
                ));

        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        if (directoryName == null || directoryName.isBlank()) {
            throw new ValidationException(
                    "Directory name is required",
                    "ERR_DIRECTORY_NAME_REQUIRED"
            );
        }

        String cleanedDirectoryName = directoryName.trim();

        if (directoryRepository
                .existsByProjectIdAndDirectoryNameIgnoreCaseAndIsDeletedFalse(
                        projectId,
                        cleanedDirectoryName
                )) {
            throw new ValidationException(
                    "Directory already exists in this project",
                    "ERR_DIRECTORY_ALREADY_EXISTS"
            );
        }

        ProjectDirectory directory = new ProjectDirectory();
        directory.setProject(project);
        directory.setDirectoryName(cleanedDirectoryName);
        directory.setCreatedBy(userId);
        directory.setCreatedDate(new Date());
        directory.setDeleted(false);

        directory = directoryRepository.save(directory);

        return mapResponse(directory);
    }

    @Override
    public ProjectDirectoryResponseDto uploadDocuments(
            Long projectId,
            Long directoryId,
            Long userId,
            List<MultipartFile> files
    ) {
        userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        "ERR_USER_NOT_FOUND"
                ));

        ProjectDirectory directory = directoryRepository
                .findByIdAndIsDeletedFalse(directoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project directory not found",
                        "ERR_PROJECT_DIRECTORY_NOT_FOUND"
                ));

        if (!directory.getProject().getId().equals(projectId)) {
            throw new ValidationException(
                    "Directory does not belong to this project",
                    "ERR_DIRECTORY_PROJECT_MISMATCH"
            );
        }

        if (files == null || files.isEmpty()) {
            throw new ValidationException(
                    "At least one document is required",
                    "ERR_DOCUMENT_REQUIRED"
            );
        }

        List<Document> uploadedDocuments = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            try {
                String s3Key = s3Service.uploadFile(file);
                String fileUrl = s3Service.getFullUrl(s3Key);

                Document document = new Document();
                document.setUuid(UUID.randomUUID().toString());
                document.setFileName(file.getOriginalFilename());
                document.setFileUrl(fileUrl);

                uploadedDocuments.add(document);

            } catch (IOException exception) {
                throw new RuntimeException(
                        "Failed to upload document: "
                                + file.getOriginalFilename(),
                        exception
                );
            }
        }

        if (uploadedDocuments.isEmpty()) {
            throw new ValidationException(
                    "No valid documents were provided",
                    "ERR_NO_VALID_DOCUMENT"
            );
        }

        documentRepository.saveAll(uploadedDocuments);
        directory.getDocuments().addAll(uploadedDocuments);

        directory = directoryRepository.save(directory);

        return mapResponse(directory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDirectoryResponseDto> getProjectDirectories(
            Long projectId
    ) {
        return directoryRepository
                .findByProjectIdAndIsDeletedFalseOrderByCreatedDateDesc(projectId)
                .stream()
                .map(this::mapResponse)
                .toList();
    }

    private ProjectDirectoryResponseDto mapResponse(
            ProjectDirectory directory
    ) {
        ProjectDirectoryResponseDto response =
                new ProjectDirectoryResponseDto();

        response.setDirectoryId(directory.getId());
        response.setProjectId(directory.getProject().getId());
        response.setDirectoryName(directory.getDirectoryName());
        response.setCreatedBy(directory.getCreatedBy());

        List<DocumentUploadResponse> documents =
                directory.getDocuments()
                        .stream()
                        .map(this::mapDocument)
                        .toList();

        response.setDocuments(documents);

        return response;
    }

    private DocumentUploadResponse mapDocument(Document document) {
        DocumentUploadResponse response =
                new DocumentUploadResponse();

        response.setId(document.getId());
        response.setUuid(document.getUuid());
        response.setFileName(document.getFileName());
        response.setUrl(document.getFileUrl());

        return response;
    }
}