//package com.doc.entity.project.activity;
//
//import com.doc.entity.project.Project;
//import com.doc.entity.project.ProjectActivity;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
//
//@Entity
//@Data
//@Table(name = "project_email")
//public class ProjectEmail {
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
//    @Column(name = "subject", nullable = false)
//    private String subject;
//
//    @Column(name = "email_to")
//    private String emailTo;
//
//    @Column(name = "email_cc")
//    private String emailCc;
//
//    @Column(name = "email_bcc")
//    private String emailBcc;
//
//    @Column(name = "body", columnDefinition = "TEXT")
//    private String body;
//
//    @Column(name = "delivery_status", length = 30)
//    private String deliveryStatus; // SENT / FAILED / DRAFT
//
//    @Column(name = "sent_at")
//    private LocalDateTime sentAt;
//
//}
