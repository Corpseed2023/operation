package com.doc.entity.project;


import com.doc.em.ActivityType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_activity", indexes = {
        @Index(name = "idx_pa_project", columnList = "project_id"),
        @Index(name = "idx_pa_type", columnList = "activity_type"),
        @Index(name = "idx_pa_date", columnList = "activity_date"),
        @Index(name = "idx_pa_project_date", columnList = "project_id, activity_date")
})
@Data
@NoArgsConstructor
public class ProjectActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which project this activity belongs to
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    // Common title for list display
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    // Optional short preview/subtitle
    @Column(name = "summary", length = 500)
    private String summary;

    // For timeline sorting
    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;

    // User who created / triggered the activity
    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_by_user_name", length = 150)
    private String createdByUserName;

    // optional system-generated vs manual
    @Column(name = "is_system_generated", nullable = false)
    private boolean systemGenerated = false;


    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}
