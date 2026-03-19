//package com.doc.entity.project.activity;
//
//
//import com.doc.entity.project.Project;
//import com.doc.entity.project.ProjectActivity;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "project_reminder")
//@Data
//@NoArgsConstructor
//public class ProjectReminder {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @OneToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "activity_id", nullable = false, unique = true)
//    private ProjectActivity activity;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "project_id", nullable = false)
//    private Project project;
//
//    @Column(name = "reminder_text", nullable = false, columnDefinition = "TEXT")
//    private String reminderText;
//
//    @Column(name = "remind_at", nullable = false)
//    private LocalDateTime remindAt;
//
//    @Column(name = "status", length = 20)
//    private String status; // PENDING / DONE / MISSED / CANCELLED
//
//    @Column(name = "assigned_to_user_id")
//    private Long assignedToUserId;
//
//    @Column(name = "assigned_to_user_name")
//    private String assignedToUserName;
//
//
//
//}
