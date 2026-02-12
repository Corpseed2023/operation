package com.doc.service;

import com.doc.dto.project.ProjectResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface ProjectSearchService {

    List<ProjectResponseDto> searchProjectsByCompanyName(String companyName, Long userId);

    List<ProjectResponseDto> searchProjectsByProjectNumber(String projectNumber, Long userId);

    List<ProjectResponseDto> searchProjectsByContactName(String contactName, Long userId);

    List<ProjectResponseDto> searchProjectsByProjectName(String projectName, Long userId);

    List<ProjectResponseDto> searchProjects(String type, String value, Long userId, String statusName,LocalDate fromDate,LocalDate toDate);

}