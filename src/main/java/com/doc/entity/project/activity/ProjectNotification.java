//package com.doc.entity.project.activity;
//
//
//
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
//@Table(name = "project_notification")
//@Data
//@NoArgsConstructor
//
//public class ProjectNotification {
//
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
//    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
//    private String message;
//
//    @Column(name = "notification_type", length = 30)
//    private String notificationType;
//
//    @Column(name = "target_user_id")
//    private Long targetUserId;
//
//    @Column(name = "target_user_name")
//    private String targetUserName;
//
//    @Column(name = "is_read", nullable = false)
//    private boolean read = false;
//
//    @Column(name = "sent_at", nullable = false)
//    private LocalDateTime sentAt;
//}
