//package com.doc.entity.project.activity;
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
//@Table(name = "project_chat")
//@Data
//@NoArgsConstructor
//public class ProjectChat {
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
//    @Column(name = "chat_channel", length = 30)
//    private String chatChannel; // WHATSAPP / INTERNAL / SMS
//
//    @Column(name = "message_status", length = 30)
//    private String messageStatus;
//
//    @Column(name = "sent_at", nullable = false)
//    private LocalDateTime sentAt;
//}
