package com.doc.impl;

import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.dto.project.report.MilestoneSummaryDto;
import com.doc.dto.project.report.ProjectActivitySummaryDto;
import com.doc.em.ActivityType;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectActivity;
import com.doc.entity.project.ProjectMilestoneAssignment;
import com.doc.entity.project.activity.ProjectNote;
import com.doc.repository.ProjectAssignmentHistoryRepository;
import com.doc.repository.ProjectMilestoneAssignmentRepository;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserProductMapRepository;
import com.doc.repository.projectRepo.activity.ProjectActivityRepository;
import com.doc.repository.projectRepo.activity.ProjectNoteRepository;
import com.doc.service.ActivityService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityServiceServiceImpl implements ActivityService {

    @Autowired private ProjectMilestoneAssignmentRepository projectMilestoneAssignmentRepository;

    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectAssignmentHistoryRepository projectAssignmentHistoryRepository;
    @Autowired private UserProductMapRepository userProductMapRepository;
    @Autowired private ProjectNoteRepository projectNoteRepository;


    @Autowired
    private ProjectActivityRepository projectActivityRepository;


    @Override
    public ProjectActivitySummaryDto getProjectSummary(Long projectId) {


        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<ProjectActivity> activities =
                projectActivityRepository.findByProjectIdAndDeletedFalse(projectId);


        List<ProjectMilestoneAssignment> milestones =
                projectMilestoneAssignmentRepository.findByProjectIdAndIsDeletedFalse(projectId);

        ProjectActivitySummaryDto dto = new ProjectActivitySummaryDto();


        dto.setProjectStatus(project.getStatus().getName());


        List<MilestoneSummaryDto> milestoneDtos = milestones.stream().map(m -> {

            MilestoneSummaryDto ms = new MilestoneSummaryDto();

            ms.setMilestoneName(m.getMilestone().getName());

            if (m.getAssignedUser() != null) {
                ms.setAssigneeUserId(m.getAssignedUser().getId());
                ms.setAssigneeUserName(m.getAssignedUser().getFullName());
            }

            if (m.getStatus() != null) {
                ms.setStatus(m.getStatus().getName());
            }

            return ms;

        }).toList();

        dto.setMilestones(milestoneDtos);



        List<Long> noteActivityIds = activities.stream()
                .filter(a -> a.getActivityType() == ActivityType.NOTE)
                .map(ProjectActivity::getId)
                .toList();


        List<ProjectNote> notesList = projectNoteRepository.findByActivity_IdIn(noteActivityIds);

        Map<Long, ProjectNote> noteMap = notesList.stream()
                .collect(Collectors.toMap(
                        n -> n.getActivity().getId(),
                        n -> n
                ));


        List<ProjectActivityResponseDto> notes = activities.stream()
                .filter(a -> a.getActivityType() == ActivityType.NOTE)
                .map(a -> {
                    ProjectActivityResponseDto dtoItem = new ProjectActivityResponseDto();

                    dtoItem.setActivityId(a.getId());
                    dtoItem.setActivityType(a.getActivityType());
                    dtoItem.setTitle(a.getTitle());

                    ProjectNote note = noteMap.get(a.getId());
                    dtoItem.setSummary(note != null ? note.getNoteText() : null);

                    dtoItem.setActivityDate(a.getCreatedDate());
                    dtoItem.setCreatedByUserId(a.getCreatedByUserId());
                    dtoItem.setCreatedByUserName(
                            a.getCreatedByUserName() != null ? a.getCreatedByUserName() : "Unknown"
                    );

                    return dtoItem;
                })
                .toList();

        List<ProjectActivityResponseDto> comments = activities.stream()
                .filter(a -> a.getActivityType() == ActivityType.COMMENT)
                .map(a -> {
                    ProjectActivityResponseDto dtoItem = new ProjectActivityResponseDto();

                    dtoItem.setActivityId(a.getId());
                    dtoItem.setActivityType(a.getActivityType());
                    dtoItem.setTitle(a.getTitle());
                    dtoItem.setSummary(a.getSummary());

                    dtoItem.setActivityDate(a.getCreatedDate());
                    dtoItem.setCreatedByUserId(a.getCreatedByUserId());
                    dtoItem.setCreatedByUserName(
                            a.getCreatedByUserName() != null ? a.getCreatedByUserName() : "Unknown"
                    );

                    return dtoItem;
                })
                .toList();

        dto.setNotes(notes);
        dto.setComments(comments);

        return dto;
    }

}
