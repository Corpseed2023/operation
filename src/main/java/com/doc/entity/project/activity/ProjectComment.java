package com.doc.entity.project.activity;

import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectActivity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "project_comment")
@Data
@NoArgsConstructor
public class ProjectComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false, unique = true)
    private ProjectActivity activity;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    // for threaded comment if needed
    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    private Long createdByUserId;
    private String createdByUserName;
}
